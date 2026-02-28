package com.example.demo.common; // 這裡請換成你自己的 package 名稱

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct; // Spring Boot 3 請用 jakarta
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // 讀取 resources 資料夾下的金鑰
            InputStream serviceAccount = new ClassPathResource("serviceAccountKey.json").getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // 確保只初始化一次
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase Admin SDK 初始化成功！");
            }
        } catch (Exception e) {
            System.err.println("❌ Firebase 初始化失敗：" + e.getMessage());
            e.printStackTrace();
        }
    }
}