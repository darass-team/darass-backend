package com.darass.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.darass.MockSpringContainerTest;
import com.darass.auth.domain.KaKaoOAuthProvider;
import com.darass.auth.dto.AccessTokenResponse;
import com.darass.auth.dto.TokenRequest;
import com.darass.auth.dto.TokenResponse;
import com.darass.auth.infrastructure.JwtTokenProvider;
import com.darass.exception.ExceptionWithMessageAndCode;
import com.darass.user.domain.SocialLoginUser;
import java.util.Optional;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("OAuthService 클래스")
class OAuthServiceTest extends MockSpringContainerTest {

    private static final String AUTHORIZATION_CODE = "LiCNQrImAFxi3LJAdt9ipGMSeOhmR4hw33Ao9cx6jkvW5w";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private OAuthService oAuthService;

    @Autowired
    private EntityManager entityManager;

    private SocialLoginUser socialLoginUser;

    @BeforeEach
    void setUp() {
        socialLoginUser = SocialLoginUser
            .builder()
            .nickName("우기")
            .oauthId("6752453")
            .oauthProvider("kakao")
            .email("jujubebat@kakao.com")
            .profileImageUrl("http://kakao/profile_image.png")
            .build();

        given(oAuthProviderFactory.getOAuthProvider(any())).willReturn(kaKaoOAuthProvider);
        given(kaKaoOAuthProvider.requestSocialLoginUser(any())).willReturn(socialLoginUser);
    }

    @DisplayName("(회원가입) login 메서드는 oauth 토큰이 주어지면, 인증서버에서 사용자 정보를 받아와서 DB 저장을 하고 primary key를 payload 삼아 accessToken을 리턴한다.")
    @Test
    void oauthLogin_register() {
        //given
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);

        //then
        TokenResponse tokenResponse = oAuthService.oauthLogin(tokenRequest);

        //when
        String accessTokenPayload = jwtTokenProvider.getAccessTokenPayload(tokenResponse.getAccessToken());
        Optional<SocialLoginUser> possibleSocialLoginUser = socialLoginUserRepository
            .findById(Long.parseLong(accessTokenPayload));
        assertThat(possibleSocialLoginUser.isPresent()).isTrue();

        SocialLoginUser socialLoginUser = possibleSocialLoginUser.get();
        assertThat(socialLoginUser.getRefreshToken()).isEqualTo(tokenResponse.getRefreshToken());
    }

    @DisplayName("(로그인 - 이미 DB에 회원정보가 있는경우) login 메서드는 oauth 토큰이 주어지면, 인증서버에서 사용자 정보를 받아와서 DB 저장을 하고 primary key를 payload 삼아 accessToken을 리턴한다.")
    @Test
    void oauthLogin_login() {
        //given
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);

        //then
        oAuthService.oauthLogin(tokenRequest);
        TokenResponse tokenResponse = oAuthService.oauthLogin(tokenRequest);

        //when
        String accessTokenPayload = jwtTokenProvider.getAccessTokenPayload(tokenResponse.getAccessToken());
        Optional<SocialLoginUser> possibleSocialLoginUser = socialLoginUserRepository
            .findById(Long.parseLong(accessTokenPayload));
        assertThat(possibleSocialLoginUser.isPresent()).isTrue();

        SocialLoginUser socialLoginUser = possibleSocialLoginUser.get();
        assertThat(socialLoginUser.getAccessToken()).isEqualTo(tokenResponse.getAccessToken());
        assertThat(socialLoginUser.getRefreshToken()).isEqualTo(tokenResponse.getRefreshToken());
    }

    @DisplayName("findSocialLoginUserByAccessToken 메서드는 accessToken이 주어지면 SocialLoginUser를 리턴한다.")
    @Test
    void findSocialLoginUserByAccessToken() {
        //given
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);
        TokenResponse tokenResponse = oAuthService.oauthLogin(tokenRequest);

        //then
        SocialLoginUser result = oAuthService.findSocialLoginUserByAccessToken(tokenResponse.getAccessToken());

        //when
        assertThat(result.getId()).isEqualTo(socialLoginUser.getId());
        assertThat(result.getNickName()).isEqualTo(socialLoginUser.getNickName());
        assertThat(result.getProfileImageUrl()).isEqualTo(socialLoginUser.getProfileImageUrl());
        assertThat(result.getOauthProvider()).isEqualTo(socialLoginUser.getOauthProvider());
        assertThat(result.getEmail()).isEqualTo(socialLoginUser.getEmail());
    }

    @DisplayName("findSocialLoginUserByAccessToken 메서드는 accessToken이 db에 존재하지 않는다면, 예외를 던진다.")
    @Test
    void findSocialLoginUserByAccessToken_exception() {
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);
        TokenResponse tokenResponse = oAuthService.oauthLogin(tokenRequest);
        socialLoginUserRepository.deleteAll();

        assertThatThrownBy(() -> oAuthService.findSocialLoginUserByAccessToken(tokenResponse.getAccessToken()))
                .isInstanceOf(ExceptionWithMessageAndCode.INVALID_JWT_NOT_FOUND_USER_TOKEN.getException().getClass());
    }

    @DisplayName("유효한 refreshToken이 주어지고, DB의 accessToken이 유효하지 않다면 accessToken을 발급해준다.")
    @Transactional
    @Test
    void getAccessTokenWithRefreshToken() throws InterruptedException {
        //given
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);
        TokenResponse tokenResponse = oAuthService.oauthLogin(tokenRequest);
        socialLoginUser.updateAccessToken(null);
        entityManager.flush();

        Thread.sleep(1000);

        //when
        AccessTokenResponse accessTokenResponse = oAuthService.getAccessTokenWithRefreshToken(tokenResponse.getRefreshToken());

        //then
        assertThat(tokenResponse.getAccessToken()).isNotEqualTo(accessTokenResponse.getAccessToken());
    }

    @DisplayName("refreshAccessTokenWithRefreshToken 메서드는 refreshToken이 db에 존재하지 않는다면, 예외를 던진다.")
    @Test
    void refreshAccessTokenWithRefreshToken_exception() {
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);
        TokenResponse tokenResponse = oAuthService.oauthLogin(tokenRequest);
        socialLoginUserRepository.deleteAll();

        assertThatThrownBy(() -> oAuthService.getAccessTokenWithRefreshToken(tokenResponse.getRefreshToken()))
            .isInstanceOf(ExceptionWithMessageAndCode.SHOULD_LOGIN.getException().getClass());
    }

    @DisplayName("로그아웃을 하면 액세스 토큰과 리프레시 토큰이 null이 된다.")
    @Test
    void log_out() {
        TokenRequest tokenRequest = new TokenRequest(KaKaoOAuthProvider.NAME, AUTHORIZATION_CODE);
        oAuthService.oauthLogin(tokenRequest);

        oAuthService.logOut(socialLoginUser);
        assertThat(socialLoginUser.getAccessToken()).isNull();
        assertThat(socialLoginUser.getRefreshToken()).isNull();
    }

}