package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public UserDTOResponse saveOrUpdateUser(SaveAuth0UserRequestDTO request) {

        //provjera jel ga vec imamo
        User existing = userRepository.findByEmail(request.email()).orElse(null);

        if (existing != null) {
            // Update ako vec postoji
            existing.setAuth0Id(request.auth0Id());
            existing.setName(request.name());
            existing.setSurname(request.surname());
            existing.setLastLogin(LocalDateTime.now());

            User savedUser = userRepository.save(existing);
            return UserMapper.toDTO(savedUser);
        }

        // stvaramo ga ako ne postoji
        User newUser = UserMapper.toEntity(request);
        User savedUser = userRepository.save(newUser);
        return UserMapper.toDTO(savedUser);
    }

    public List<UserDTOResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }
}
