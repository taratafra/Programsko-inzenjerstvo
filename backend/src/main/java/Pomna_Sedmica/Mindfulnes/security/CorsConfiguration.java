package Pomna_Sedmica.Mindfulnes.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class CorsConfiguration {

    public void corsConfigurationSource(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors( c -> {
            CorsConfigurationSource source = s -> {
                org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                config.addAllowedOrigin("http://localhost:3000");
                config.setAllowCredentials(true);
                return config;
            };
            c.configurationSource(source);
        });
    }
}




