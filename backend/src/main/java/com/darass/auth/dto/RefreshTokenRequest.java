package com.darass.auth.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotNull(message = "refreshToken은 비어 있으면 안 됩니다.")
    private String refreshToken;

}
