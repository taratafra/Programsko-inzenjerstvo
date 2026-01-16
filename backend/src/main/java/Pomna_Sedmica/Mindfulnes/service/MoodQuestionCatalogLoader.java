package Pomna_Sedmica.Mindfulnes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MoodQuestionCatalogLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode root;

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource("mood_questions.json").getInputStream()) {
            root = mapper.readTree(in);
            System.out.println("Mood questions loaded (" + root.path("questions").size() + " items)");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load mood_questions.json", e);
        }
    }

    public JsonNode root() {
        return root;
    }
}
