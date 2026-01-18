package Pomna_Sedmica.Mindfulnes.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Cors;
import com.google.cloud.storage.HttpMethod; // <--- OVO JE FALILO
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                // Pazi da je ovdje tvoje točno ime bucketa
                .setStorageBucket("mindful-test-3.firebasestorage.app")
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Bucket storageBucket(FirebaseApp firebaseApp) {
        Bucket bucket = StorageClient.getInstance(firebaseApp).bucket();

        // --- CORS FIX ---
        try {
            Cors cors = Cors.newBuilder()
                    .setOrigins(List.of(Cors.Origin.of("*")))
                    .setMethods(List.of(HttpMethod.GET)) // <--- POPRAVLJENO (HttpMethod umjesto Cors.Method)
                    .setResponseHeaders(Collections.singletonList("Content-Type"))
                    .setMaxAgeSeconds(3600)
                    .build();

            bucket.toBuilder().setCors(List.of(cors)).build().update();
            System.out.println("✅ CORS pravila su uspješno postavljena!");
        } catch (Exception e) {
            System.err.println("⚠️ Greška pri postavljanju CORS-a: " + e.getMessage());
        }
        // ----------------

        return bucket;
    }
}