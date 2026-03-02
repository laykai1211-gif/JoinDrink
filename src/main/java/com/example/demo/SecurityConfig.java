package com.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 禁用 CSRF (API 開發必做)
                .csrf(AbstractHttpConfigurer::disable)

                // 2. 允許跨域 (為了讓 Vue 前端能存取)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. 禁用預設的登入表單與視窗，防止彈出隨機密碼要求
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 4. 設定權限控管
                .authorizeHttpRequests(auth -> auth
                        // 💡 確保 /api/auth/ 下的所有路徑（包括重設密碼）都對外公開
                        .requestMatchers("/api/auth/**").permitAll()
                        // 其他所有請求都需要驗證 (如果之後有加 JWT Filter)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // 💡 簡單的 CORS 配置，允許前端 5173 埠存取
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173")); // Vue 預設埠
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}