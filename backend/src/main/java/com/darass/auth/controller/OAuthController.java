package com.darass.auth.controller;

import com.darass.auth.domain.RequiredLogin;
import com.darass.auth.dto.AccessTokenResponse;
import com.darass.auth.dto.RefreshTokenRequest;
import com.darass.auth.dto.TokenRequest;
import com.darass.auth.dto.TokenResponse;
import com.darass.auth.service.OAuthService;
import com.darass.user.domain.SocialLoginUser;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1")
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping("/login/oauth")
    public ResponseEntity<TokenResponse> oauthLogin(@RequestBody @Valid TokenRequest tokenRequest) {
        return ResponseEntity.status(HttpStatus.OK).body(oAuthService.oauthLogin(tokenRequest));
    }

    @PostMapping("/login/refresh")
    public ResponseEntity<AccessTokenResponse> regenerateAccessToken(@RequestBody @Valid RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(oAuthService.getAccessTokenWithRefreshToken(refreshTokenRequest.getRefreshToken()));
    }

    @DeleteMapping("/log-out")
    public ResponseEntity<Void> logOut(@RequiredLogin SocialLoginUser socialLoginUser) {
        oAuthService.logOut(socialLoginUser);
        return ResponseEntity.noContent().build();
    }

}
