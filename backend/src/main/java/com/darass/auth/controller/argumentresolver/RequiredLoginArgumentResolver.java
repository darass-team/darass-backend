package com.darass.auth.controller.argumentresolver;

import com.darass.auth.domain.RequiredLogin;
import com.darass.auth.infrastructure.AuthorizationExtractor;
import com.darass.auth.service.OAuthService;
import com.darass.exception.ExceptionWithMessageAndCode;
import com.darass.user.domain.SocialLoginUser;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
public class RequiredLoginArgumentResolver implements HandlerMethodArgumentResolver {

    private final OAuthService oAuthService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequiredLogin.class);
    }

    @Override
    public SocialLoginUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String accessToken = findAccessToken(webRequest);
        return oAuthService.findSocialLoginUserByAccessToken(accessToken);
    }

    private String findAccessToken(NativeWebRequest webRequest) {
        String accessToken = AuthorizationExtractor
            .extract(Objects.requireNonNull(webRequest.getNativeRequest(HttpServletRequest.class)));

        if (Objects.isNull(accessToken) || accessToken.isEmpty()) {
            throw ExceptionWithMessageAndCode.NOT_EXISTS_ACCESS_TOKEN.getException();
        }
        return accessToken;
    }

}
