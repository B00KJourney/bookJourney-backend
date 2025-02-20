package com.example.bookjourneybackend.domain.auth.service;

import com.example.bookjourneybackend.domain.auth.domain.dto.request.PostAuthAccessTokenReissueRequest;
import com.example.bookjourneybackend.domain.auth.domain.dto.request.PostAuthLoginRequest;
import com.example.bookjourneybackend.domain.auth.domain.dto.response.PostAuthAccessTokenReissueResponse;
import com.example.bookjourneybackend.domain.auth.domain.dto.response.PostAuthLoginResponse;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.global.exception.GlobalException;
import com.example.bookjourneybackend.global.util.JwtAuthenticationFilter;
import com.example.bookjourneybackend.global.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.example.bookjourneybackend.global.entity.EntityStatus.ACTIVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;


    private User mockUser;
    String email = "test@example.com";
    String password = "password123";
    String nickName = "testUser";
    Long userId = 1L;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email(email)
                .password(password)
                .nickname(nickName)
                .build();
    }


    @Test
    @DisplayName("로그인_성공")
    void loginSuccess() {

        // given
        PostAuthLoginRequest loginRequest = new PostAuthLoginRequest(email, password);

        when(userRepository.findByEmailAndStatus(email,ACTIVE))
                .thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(password, mockUser.getPassword()))
                .thenReturn(true);
        when(jwtUtil.createAccessToken(mockUser.getUserId()))
                .thenReturn("mockAccessToken");
        when(jwtUtil.createRefreshToken(mockUser.getUserId()))
                .thenReturn("mockRefreshToken");

        // when
        PostAuthLoginResponse postAuthLoginResponse = authService.login(loginRequest, request, response);

        // then
        assertNotNull(postAuthLoginResponse);
        assertEquals("mockAccessToken", postAuthLoginResponse.getAccessToken());
        assertEquals("mockRefreshToken", postAuthLoginResponse.getRefreshToken());

        verify(jwtAuthenticationFilter).setAuthentication(request, mockUser.getUserId());
        verify(redisService).storeRefreshToken("mockRefreshToken", mockUser.getUserId());
    }

    @Test
    @DisplayName("로그인_실패_이메일_없음")
    void loginFailureEmailNotFound() {

        // given
        PostAuthLoginRequest loginRequest = new PostAuthLoginRequest(email, password);

        when(userRepository.findByEmailAndStatus(email, ACTIVE))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(GlobalException.class, () -> authService.login(loginRequest, request, response));
    }

    @Test
    @DisplayName("로그인_실패_비밀번호_불일치")
    void loginFailurePasswordMismatch() {

        // given
        User mockUser = new User(userId, email, passwordEncoder.encode("correctPassword"), nickName);

        PostAuthLoginRequest loginRequest = new PostAuthLoginRequest(email, password);

        when(userRepository.findByEmailAndStatus(email, ACTIVE))
                .thenReturn(Optional.of(mockUser));

        when(passwordEncoder.matches(password, mockUser.getPassword()))
                .thenReturn(false);

        // when & then
        assertThrows(GlobalException.class, () -> authService.login(loginRequest, request, response));
    }

    @Test
    @DisplayName("엑세스_토큰_재발급_성공")
    void accessTokenReissueSuccess() {

        // given
        User mockUser = new User(userId, email, passwordEncoder.encode(password), nickName);
        String oldRefreshToken = "validRefreshToken";
        String newAccessToken = "newAccessToken";

        PostAuthAccessTokenReissueRequest postAuthAccessTokenReissueRequest = new PostAuthAccessTokenReissueRequest(oldRefreshToken);

        when(jwtUtil.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtUtil.extractUserIdFromJwtToken(oldRefreshToken)).thenReturn(mockUser.getUserId());
        when(redisService.checkTokenExists(mockUser.getUserId().toString())).thenReturn(true);
        when(jwtUtil.createAccessToken(mockUser.getUserId())).thenReturn(newAccessToken);

        // when
        PostAuthAccessTokenReissueResponse postAuthAccessTokenReissueResponse = authService.tokenReissue(postAuthAccessTokenReissueRequest, this.response, this.request);

        // then
        assertNotNull(postAuthAccessTokenReissueResponse);
        assertEquals(newAccessToken, postAuthAccessTokenReissueResponse.getAccessToken());
        verify(jwtAuthenticationFilter).setAuthentication(request, userId);

    }

    @Test
    @DisplayName("엑세스_토큰_재발급_실패_리프레시_토큰_만료")
    void accessTokenReissueFailureRefreshTokenExpired() {

        // given
        String expiredRefreshToken = "expiredToken";
        PostAuthAccessTokenReissueRequest postAuthAccessTokenReissueRequest
                = new PostAuthAccessTokenReissueRequest(expiredRefreshToken);
        when(jwtUtil.validateToken(expiredRefreshToken)).thenReturn(false);

        // when & then
        assertThrows(GlobalException.class, () -> authService.tokenReissue(postAuthAccessTokenReissueRequest, response, this.request));
    }

    @Test
    @DisplayName("로그아웃_성공")
    void logoutSuccess() {

        // given
        User mockUser = new User(userId, email, passwordEncoder.encode(password), nickName);
        when(userRepository.findByUserIdAndStatus(userId, ACTIVE))
                .thenReturn(Optional.of(mockUser));

        // when
        authService.logout(userId);

        // then
        verify(redisService).invalidateToken(mockUser.getUserId());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

