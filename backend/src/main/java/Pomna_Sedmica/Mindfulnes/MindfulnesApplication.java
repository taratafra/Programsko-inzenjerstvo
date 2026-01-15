package Pomna_Sedmica.Mindfulnes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class MindfulnesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MindfulnesApplication.class, args);
		System.out.println("Current Working Directory: " + System.getProperty("user.dir"));
	}

}
