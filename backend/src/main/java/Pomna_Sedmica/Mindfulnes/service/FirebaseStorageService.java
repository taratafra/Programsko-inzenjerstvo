package Pomna_Sedmica.Mindfulnes.service;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Service
public class FirebaseStorageService {

    private final Bucket bucket;

    public FirebaseStorageService(Bucket bucket) {
        this.bucket = bucket;
    }

    public String uploadIfNotExists(Path localPath, String remoteFolder) {
        String fileName = localPath.getFileName().toString();
        String blobName = remoteFolder + "/" + fileName;

        System.out.println("[Firebase] Checking file: " + blobName);

        try {
            // Check if file already exists
            Blob blob = bucket.get(blobName);
            if (blob != null && blob.exists()) {
                System.out.println("[Firebase] Already exists: " + fileName);
                return blobName;
            }

            // Upload if doesn't exist
            System.out.println("[Firebase] Uploading: " + fileName + " to bucket: " + bucket.getName());
            try (InputStream inputStream = Files.newInputStream(localPath)) {
                String contentType = Files.probeContentType(localPath);
                if (contentType == null) contentType = "application/octet-stream";

                Blob newBlob = bucket.create(blobName, inputStream, contentType);
                System.out.println("[Firebase] Successfully uploaded: " + fileName);

                return blobName;
            }
        } catch (IOException e) {
            System.err.println("[Firebase] Failed to upload " + fileName + ": " + e.getMessage());
            throw new RuntimeException("Failed to upload to Firebase: " + fileName, e);
        } catch (Exception e) {
            System.err.println("[Firebase] Unexpected error for " + fileName + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Unexpected error uploading to Firebase: " + fileName, e);
        }
    }

    public void makeAllFilesPublic() {
        System.out.println("[Firebase] Uniform Bucket-Level Access enabled. Skipping ACL updates.");
    }
}
