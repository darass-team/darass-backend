package com.darass.auth.infrastructure;

import com.darass.exception.ExceptionWithMessageAndCode;
import com.darass.user.domain.SocialLoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    @Value("${security.jwt.access-token.secret-key}")
    private String secretKeyOfAccessToken;

    @Value("${security.jwt.refresh-token.secret-key}")
    private String secretKeyOfRefreshToken;

    @Value("${security.jwt.access-token.expire-length}")
    private long validityInMillisecondsOfAccessToken;

    @Value("${security.jwt.refresh-token.expire-length}")
    private long validityInMillisecondsOfRefreshToken;

    public String createAccessToken(SocialLoginUser socialLoginUser) {
        if (isValidateToken(socialLoginUser.getAccessToken(), secretKeyOfAccessToken)) {
            return socialLoginUser.getAccessToken();
        }

        Claims claims = Jwts.claims().setSubject(socialLoginUser.getId().toString());
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMillisecondsOfAccessToken);

        String accessToken = createJwtToken(claims, now, validity, secretKeyOfAccessToken);
        socialLoginUser.updateAccessToken(accessToken);
        return accessToken;
    }

    public String createRefreshToken(SocialLoginUser socialLoginUser) {
        if (isValidateToken(socialLoginUser.getRefreshToken(), secretKeyOfRefreshToken)) {
            return socialLoginUser.getRefreshToken();
        }

        Claims claims = Jwts.claims().setSubject(socialLoginUser.getId().toString());
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMillisecondsOfRefreshToken);

        String refreshToken = createJwtToken(claims, now, validity, secretKeyOfRefreshToken);
        socialLoginUser.updateRefreshToken(refreshToken);
        return refreshToken;
    }

    public boolean isValidatedAccessToken(String accessToken) {
        return isValidateToken(accessToken, secretKeyOfAccessToken);
    }

    public void validateAccessToken(String accessToken) {
        if (!isValidateToken(accessToken, secretKeyOfAccessToken)) {
            throw ExceptionWithMessageAndCode.INVALID_ACCESS_TOKEN.getException();
        }
    }

    public void validateRefreshToken(String refreshToken) {
        if (!isValidateToken(refreshToken, secretKeyOfRefreshToken)) {
            throw ExceptionWithMessageAndCode.INVALID_REFRESH_TOKEN.getException();
        }
    }

    public String getAccessTokenPayload(String accessToken) {
        try {
            return Jwts.parser().setSigningKey(secretKeyOfAccessToken).parseClaimsJws(accessToken).getBody().getSubject();
        } catch (MalformedJwtException e) {
            throw ExceptionWithMessageAndCode.INVALID_ACCESS_TOKEN.getException();
        }
    }

    private String createJwtToken(Claims claims, Date now, Date validity, String secretKey) {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    private boolean isValidateToken(String token, String secretKey) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
