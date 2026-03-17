package com.shipment.shippinggo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.config.path:}")
    private String configPath;

    @PostConstruct
    public void initialize() {
        try {
            if (configPath == null || configPath.isEmpty()) {
                System.out.println("Firebase config path is empty. Notifications will be disabled.");
                return;
            }

            InputStream serviceAccount;

            if (configPath.startsWith("classpath:")) {
                // Load from classpath (src/main/resources)
                String resourcePath = configPath.replace("classpath:", "");
                Resource resource = new ClassPathResource(resourcePath);
                if (!resource.exists()) {
                    System.out.println("Firebase service account file not found in classpath: " + resourcePath);
                    System.out.println("Notifications will be disabled. Place your serviceAccountKey.json in src/main/resources/");
                    return;
                }
                serviceAccount = resource.getInputStream();
            } else {
                // Load from file system (absolute path)
                Resource resource = new FileSystemResource(configPath);
                if (!resource.exists()) {
                    System.out.println("Firebase service account file not found: " + configPath);
                    System.out.println("Notifications will be disabled.");
                    return;
                }
                serviceAccount = resource.getInputStream();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase initialized successfully!");
            }
        } catch (IOException e) {
            System.err.println("Error initializing Firebase: " + e.getMessage());
            System.err.println("Notifications will be disabled.");
        }
    }
}
