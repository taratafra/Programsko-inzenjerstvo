package Pomna_Sedmica.Mindfulnes.config;

import Pomna_Sedmica.Mindfulnes.domain.entity.*;
import Pomna_Sedmica.Mindfulnes.domain.enums.ContentType;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.*;
import Pomna_Sedmica.Mindfulnes.service.FirebaseStorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(
            VideoRepository videoRepository,
            TrainerRepository trainerRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            FirebaseStorageService firebaseService
    ) {
        return args -> {
            System.out.println("=== Starting Data Initialization ===");

            // Ensure all files are public (fixes 412 error)
            firebaseService.makeAllFilesPublic();

            try {
                // Create or fetch specific trainers
                List<Trainer> trainers = createTrainers(userRepository, trainerRepository, passwordEncoder);
                System.out.println("Trainers ready: " + trainers.size());

                if (trainers.isEmpty()) {
                    System.err.println("No trainers available! Cannot proceed.");
                    return;
                }

                if (videoRepository.count() > 0) {
                    System.out.println("Data already exists. Reassigning content to the 3 coaches...");
                    reassignContentToTrainers(videoRepository, trainers);
                } else {
                    // Process videos
                    processVideos(videoRepository, firebaseService, trainers);

                    // Process audio
                    processAudio(videoRepository, firebaseService, trainers);

                    // Process blogs
                    processBlogs(videoRepository, firebaseService, trainers);
                }

                System.out.println("=== Data Initialization Complete ===");
            } catch (Exception e) {
                System.err.println("Error during data initialization: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private List<Trainer> createTrainers(UserRepository userRepository, TrainerRepository trainerRepository, PasswordEncoder passwordEncoder) {
        String[][] trainerData = {
                {"Ana", "Anić", "ana.anic@mindfulness.com"},
                {"Ivan", "Ivić", "ivan.ivic@mindfulness.com"},
                {"Marko", "Marić", "marko.maric@mindfulness.com"}
        };

        List<Trainer> trainers = new ArrayList<>();

        for (String[] data : trainerData) {
            Optional<User> existingUser = userRepository.findByEmail(data[2]);
            if (existingUser.isPresent()) {
                if (existingUser.get() instanceof Trainer) {
                    trainers.add((Trainer) existingUser.get());
                }
                continue;
            }

            Trainer trainer = new Trainer();
            trainer.setName(data[0]);
            trainer.setSurname(data[1]);
            trainer.setEmail(data[2]);
            trainer.setPassword(passwordEncoder.encode("trainer123"));
            trainer.setRole(Role.TRAINER);
            trainer.setOnboardingComplete(true);
            trainer.setApproved(true);

            trainers.add(trainerRepository.save(trainer));
            System.out.println("Created trainer: " + data[0] + " " + data[1]);
        }

        return trainers;
    }

    private void reassignContentToTrainers(VideoRepository videoRepository, List<Trainer> trainers) {
        List<Video> allContent = videoRepository.findAll();
        for (Video content : allContent) {
            content.setTrainer(getRandomElement(trainers));
            videoRepository.save(content);
        }
        System.out.println("Reassigned " + allContent.size() + " items to random trainers.");
    }

    private void processVideos(VideoRepository videoRepository, FirebaseStorageService firebaseService, List<Trainer> trainers) {
        Path videoFolder = Paths.get("ContentBase/videi");
        if (!Files.exists(videoFolder)) {
            System.out.println("Video folder not found: " + videoFolder);
            return;
        }

        try (Stream<Path> paths = Files.walk(videoFolder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String url = firebaseService.uploadIfNotExists(file, "videos");

                    Video video = new Video();
                    video.setTitle(formatTitle(file));
                    video.setUrl(url);
                    video.setType(ContentType.VIDEO);
                    
                    // Random duration between 5 and 30 minutes
                    video.setDuration(5 + new Random().nextInt(26));
                    
                    // Random Goal and Level
                    video.setGoal(getRandomElement(Arrays.asList(Goal.values())));
                    video.setLevel(getRandomElement(Arrays.asList(MeditationExperience.values())));
                    
                    video.setTrainer(getRandomElement(trainers));

                    videoRepository.save(video);
                    System.out.println("Video added: " + video.getTitle());
                } catch (Exception e) {
                    System.err.println("Error processing video " + file.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error walking video folder: " + e.getMessage());
        }
    }

    private void processAudio(VideoRepository videoRepository, FirebaseStorageService firebaseService, List<Trainer> trainers) {
        Path audioFolder = Paths.get("ContentBase/audio");
        if (!Files.exists(audioFolder)) {
            System.out.println("Audio folder not found: " + audioFolder);
            return;
        }

        try (Stream<Path> paths = Files.walk(audioFolder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String url = firebaseService.uploadIfNotExists(file, "audio");

                    Video audio = new Video();
                    audio.setTitle(formatTitle(file));
                    audio.setUrl(url);
                    audio.setType(ContentType.AUDIO);
                    
                    // Random duration between 3 and 20 minutes
                    audio.setDuration(3 + new Random().nextInt(18));

                    // Random Goal and Level
                    audio.setGoal(getRandomElement(Arrays.asList(Goal.values())));
                    audio.setLevel(getRandomElement(Arrays.asList(MeditationExperience.values())));

                    audio.setTrainer(getRandomElement(trainers));

                    videoRepository.save(audio);
                    System.out.println("Audio added: " + audio.getTitle());
                } catch (Exception e) {
                    System.err.println("Error processing audio " + file.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error walking audio folder: " + e.getMessage());
        }
    }

    private void processBlogs(VideoRepository videoRepository, FirebaseStorageService firebaseService, List<Trainer> trainers) {
        Path blogFolder = Paths.get("ContentBase/blogovi");
        if (!Files.exists(blogFolder)) {
            System.out.println("Blog folder not found: " + blogFolder);
            return;
        }

        try (Stream<Path> paths = Files.walk(blogFolder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String url = firebaseService.uploadIfNotExists(file, "blogs");

                    Video blog = new Video();
                    blog.setTitle(formatTitle(file));
                    blog.setDescription("Read our new article about: " + blog.getTitle());
                    blog.setUrl(url);
                    blog.setType(ContentType.BLOG);
                    
                    // Random duration between 5 and 15 minutes
                    blog.setDuration(5 + new Random().nextInt(11));

                    // Random Goal and Level
                    blog.setGoal(getRandomElement(Arrays.asList(Goal.values())));
                    blog.setLevel(getRandomElement(Arrays.asList(MeditationExperience.values())));

                    blog.setTrainer(getRandomElement(trainers));

                    videoRepository.save(blog);
                    System.out.println("Blog added: " + blog.getTitle());
                } catch (Exception e) {
                    System.err.println("Error processing blog " + file.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Error walking blog folder: " + e.getMessage());
        }
    }

    private String formatTitle(Path file) {
        String name = file.getFileName().toString();
        // Remove extension
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        // Replace underscores and hyphens with spaces
        name = name.replace("_", " ").replace("-", " ");
        return name;
    }

    private <T> T getRandomElement(List<T> list) {
        return list.get(new Random().nextInt(list.size()));
    }
}
