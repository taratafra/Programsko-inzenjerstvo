package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Auth0TokenResponse {
    private String access_token;
    private String id_token;
    private String refresh_token;
    private String token_type;
    private long expires_in;
}

