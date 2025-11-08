package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private boolean isSocialLogin;
}

