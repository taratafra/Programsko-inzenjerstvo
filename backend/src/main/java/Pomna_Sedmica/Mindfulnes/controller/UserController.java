package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;


    @PostMapping
    public ResponseEntity<UserDTOResponse> saveUser(@RequestBody SaveAuth0UserRequestDTO request) {
        UserDTOResponse savedUser = userService.saveOrUpdateUser(request);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping
    public ResponseEntity<List<UserDTOResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
}

