package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("회원가입 테스트")
    class SignupTests {

        @Test
        void 회원가입_성공() {
            // given
            SignupRequest signupRequest = new SignupRequest("test@example.com", "12345678!@A", "USER");
            User user = new User(signupRequest.getEmail(), "encodedPassword", UserRole.of(signupRequest.getUserRole()));
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.existsByEmail(anyString())).willReturn(false); // 이메일 중복 없음
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword"); // 비밀번호 인코딩
            given(userRepository.save(any(User.class))).willReturn(user); // 저장된 사용자 반환 (ID가 포함된 상태)
            given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("testToken"); // JWT 토큰 생성

            // when
            SignupResponse signupResponse = authService.signup(signupRequest);

            // then
            assertNotNull(signupResponse); // 회원가입 응답이 null이 아닌지 확인
            assertEquals("testToken", signupResponse.getBearerToken()); // 반환된 토큰이 예상한 값인지 확인
        }

        @Test
        void 이메일이_null일_때_예외_발생() {
            // given
            SignupRequest signupRequest = new SignupRequest(null, "12345678!@A", "USER"); // 이메일이 null인 경우

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signup(signupRequest); // 예외가 발생해야 함
            });

            // 예외 메시지 검증
            assertEquals("이메일이 비어있습니다.", exception.getMessage());
        }

        @Test
        void 이메일이_빈_문자열일_때_예외_발생() {
            // given
            SignupRequest signupRequest = new SignupRequest("", "12345678!@A", "USER"); // 이메일이 빈 문자열인 경우

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signup(signupRequest);
            });

            assertEquals("이메일이 비어있습니다.", exception.getMessage());
        }

        @Test
        void 이메일_중복() {
            // given
            SignupRequest signupRequest = new SignupRequest("test@example.com", "12345678!@A", "USER");
            given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(true);

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    authService.signup(signupRequest));

            assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class SigninTests {

        @Test
        void 이메일로_사용자를_찾지_못했을_때_예외_발생() {
            // given
            SigninRequest signinRequest = new SigninRequest("nonexistent@example.com", "password123");

            // 이메일로 사용자를 찾지 못하는 상황을 모의
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
                authService.signin(signinRequest); // 예외가 발생해야 함
            });

            // 예외 메시지 검증
            assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
        }

        @Test
        void 비밀번호가_일치하지_않을_때_예외_발생() {
            // given
            SigninRequest signinRequest = new SigninRequest("test@example.com", "wrongPassword");
            User user = new User("test@example.com", "encodedPassword", UserRole.USER);

            // 이메일로 사용자를 찾았으나, 비밀번호가 일치하지 않음
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false); // 비밀번호가 일치하지 않도록 설정

            // when & then
            AuthException exception = assertThrows(AuthException.class, () -> {
                authService.signin(signinRequest); // 예외가 발생해야 함
            });

            // 예외 메시지 검증
            assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
        }

        @Test
        void 로그인_성공() {
            // given
            SigninRequest signinRequest = new SigninRequest("test@example.com", "password123");
            User user = new User("test@example.com", "encodedPassword", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", 1L);

            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true); // 비밀번호 일치
            given(jwtUtil.createToken(anyLong(), anyString(), any(UserRole.class))).willReturn("testToken"); // JWT 토큰 생성

            // when
            SigninResponse signinResponse = authService.signin(signinRequest);

            // then
            assertNotNull(signinResponse); // 로그인 응답이 null이 아닌지 확인
            assertEquals("testToken", signinResponse.getBearerToken()); // 반환된 토큰이 예상한 값인지 확인
        }
    }
}
