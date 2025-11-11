package Pomna_Sedmica.Mindfulnes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class MessageCatalogLoader {
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode root;

    @PostConstruct
    void load() throws Exception {
        try (InputStream in = new ClassPathResource("messages/onboarding_prompts.json").getInputStream()) {
            root = mapper.readTree(in);
            System.out.println("✅ Poruke uspješno učitane (" + root.size() + " sekcija)");
        }
    }

    public JsonNode root() {
        return root;
    }
}
