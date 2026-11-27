package com.agent.biogenie.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SecondFactorTokenDTO {
    private String confirmToken;
    private long expiresInSeconds;
}
