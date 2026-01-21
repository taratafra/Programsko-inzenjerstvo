package Pomna_Sedmica.Mindfulnes.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
@Profile("dev")
public class SecurityConfigDev {

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${cors.allowed-origin:http://localhost:3000}")
    private String corsOrigin;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**","/onboarding/**","/messages/**"));
        http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/schedules/**").authenticated()
                        .requestMatchers("/api/trainers/**").permitAll()
                        .requestMatchers("/api/admins/**").authenticated()
                        .requestMatchers("/api/user/settings/**").authenticated() //trebat ce dodat jos putanje za trenera content i admina
                        .requestMatchers("/api/daily-focus/**").authenticated()
                        .requestMatchers("/api/mood-checkins/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()//mozda ne treba
                        .requestMatchers("/api/reminders/**").authenticated()//mozda ne treba
                        .requestMatchers("/public").permitAll()
                        .requestMatchers("/onboarding/**").authenticated()
                        .anyRequest().permitAll()
                )
                .cors(Customizer.withDefaults())
                // JWT Resource Server with custom decoder and converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtDecoder customJwtDecoder() {
        // Auth0 decoder
        NimbusJwtDecoder auth0Decoder = JwtDecoders.fromIssuerLocation(
                auth0Domain.endsWith("/") ? auth0Domain : auth0Domain + "/"
        );

        // Local HMAC decoder
        NimbusJwtDecoder localDecoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256"))
                .build();

        // Try Auth0 first, fallback to local
        return token -> {
            try {
                Jwt decoded = auth0Decoder.decode(token);
                System.out.println("DEBUG: Decoded via Auth0. Sub: " + decoded.getSubject());
                return decoded;
            } catch (Exception e) {
                try {
                    Jwt decoded = localDecoder.decode(token);
                    System.out.println("DEBUG: Decoded via Local. Sub: " + decoded.getSubject());
                    return decoded;
                } catch (Exception e2) {
                    System.err.println("DEBUG: Failed to decode token. Auth0 error: " + e.getMessage() + ", Local error: " + e2.getMessage());
                    throw e2;
                }
            }
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter() {
            // No @Override — works across Spring Security versions
            protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
                Collection<GrantedAuthority> authorities = new ArrayList<>();

                // Local JWT "role" claim
                Object role = jwt.getClaim("role");
                if (role != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString()));
                }

                // Auth0 scopes (optional)
                Object scope = jwt.getClaim("scope");
                if (scope instanceof String scopeStr) {
                    for (String s : scopeStr.split(" ")) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
                    }
                }

                return authorities;
            }
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
