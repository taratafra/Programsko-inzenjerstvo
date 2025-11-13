package Pomna_Sedmica.Mindfulnes.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class MindfulnesController {

    @GetMapping("/api/connect")
    public String connectMessage() {
        return "You are officially connected!";
    }
}
