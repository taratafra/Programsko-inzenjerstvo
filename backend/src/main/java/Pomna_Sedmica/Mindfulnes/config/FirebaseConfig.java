package Pomna_Sedmica.Mindfulnes.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FileInputStream serviceAccount =
                new FileInputStream("serviceAccountKey.json");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("pomna-sedmica-bucket")
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public com.google.cloud.storage.Bucket storageBucket(FirebaseApp firebaseApp) {
        return com.google.firebase.cloud.StorageClient.getInstance(firebaseApp).bucket();
    }
}
