package com.darass.user.domain;

import com.darass.exception.ExceptionWithMessageAndCode;
import com.darass.user.infrastructure.S3Service;
import java.util.Objects;
import javax.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@NoArgsConstructor
@Entity
public class SocialLoginUser extends User {

    private String email;

    private String oauthId;

    private String oauthProvider;

    private String refreshToken;

    private String accessToken;

    @Builder
    public SocialLoginUser(Long id, String nickName, String profileImageUrl, String userType, String oauthId,
        String oauthProvider, String email, String refreshToken, String accessToken) {
        super(id, nickName, profileImageUrl, userType);
        this.oauthId = oauthId;
        this.oauthProvider = oauthProvider;
        this.email = email;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }

    @Override
    public boolean isLoginUser() {
        return true;
    }

    @Override
    public boolean isAdminUser(Long id) {
        return getId().equals(id);
    }

    @Override
    public boolean isValidGuestPassword(String guestUserPassword) {
        throw ExceptionWithMessageAndCode.NOT_GUEST_USER.getException();
    }

    public void changeNickNameOrProfileImageIfExists(S3Service s3Service, String nickName, MultipartFile profileImageFile) {
        if (!Objects.isNull(nickName)) {
            changeNickName(nickName);
        }
        if (!Objects.isNull(profileImageFile)) {
            if (!getProfileImageUrl().isBlank()) {
                s3Service.delete(getProfileImageUrl());
            }
            String imageUrl = s3Service.upload(profileImageFile);
            changeProfileImageUrl(imageUrl);
        }
    }

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void deleteRefreshToken() {
        refreshToken = null;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

}
