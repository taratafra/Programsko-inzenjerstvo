package Pomna_Sedmica.Mindfulnes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class PracticePlanCatalogLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode root;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource("practice_plan_catalog.json").getInputStream()) {
            this.root = mapper.readTree(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load practice_plan_catalog.json", e);
        }
    }

    public JsonNode root() {
        return root;
    }
}
