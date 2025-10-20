package Pomna_Sedmica.Mindfulnes;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}") // allow React (frontend) to access this
public class MindfulnesController {

    @GetMapping("/api/connect")
    public String connectMessage() {
        return "You are officially connected!";
    }
}
