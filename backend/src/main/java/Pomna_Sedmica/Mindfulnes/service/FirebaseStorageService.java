package Pomna_Sedmica.Mindfulnes.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FirebaseStorageService {

    private final Bucket bucket;

    // Spring ovdje ubacuje Bucket definiran u tvom FirebaseConfig-u
    public FirebaseStorageService(Bucket bucket) {
        this.bucket = bucket;
    }

    public String uploadIfNotExists(Path localPath, String remoteFolder) {
        String fileName = localPath.getFileName().toString();
        String blobName = remoteFolder + "/" + fileName; // npr. "videos/intro.mp4"

        // 1. Provjera postoji li file
        Blob blob = bucket.get(blobName);
        if (blob != null && blob.exists()) {
            System.out.println("[Firebase] Već postoji: " + fileName);
            return generatePublicUrl(bucket.getName(), blobName);
        }

        // 2. Upload ako ne postoji
        System.out.println("[Firebase] Uploadam: " + fileName);
        try (InputStream inputStream = Files.newInputStream(localPath)) {
            String contentType = Files.probeContentType(localPath);
            if (contentType == null) contentType = "application/octet-stream";

            bucket.create(blobName, inputStream, contentType);

            return generatePublicUrl(bucket.getName(), blobName);
        } catch (IOException e) {
            throw new RuntimeException("Neuspješan upload na Firebase: " + fileName, e);
        }
    }

    private String generatePublicUrl(String bucketName, String blobName) {
        // Generira javni link
        return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                URLEncoder.encode(blobName, StandardCharsets.UTF_8));
    }
}