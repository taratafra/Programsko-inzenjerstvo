package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.Data;

@Data
public class Auth0TokenResponse {
    private String access_token;
    private String id_token;
    private String refresh_token;
    private String token_type;
    private long expires_in;
}
