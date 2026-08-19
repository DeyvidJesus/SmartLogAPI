package br.com.smartlog.infrastructure.firebase;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfiguration {

    @Bean
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
        if (properties.projectId() != null && !properties.projectId().isBlank()) {
            options.setProjectId(properties.projectId());
        }
        return FirebaseApp.initializeApp(options.build());
    }

    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }
}
