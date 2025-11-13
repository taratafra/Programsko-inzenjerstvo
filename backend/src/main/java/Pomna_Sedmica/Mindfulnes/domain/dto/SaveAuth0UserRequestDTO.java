package Pomna_Sedmica.Mindfulnes.domain.dto;

public record SaveAuth0UserRequestDTO(
        String auth0Id,
        String email,
        String name,
        String surname,
        Boolean isSocialLogin
) {}
