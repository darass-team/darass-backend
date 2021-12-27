package com.darass.auth.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

    @NotNull
    private String oauthProviderName;

    @NotNull
    private String authorizationCode;

}
