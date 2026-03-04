package com.example.demo;

import com.example.demo.common.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // 💡 注入剛寫好的 Filter

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http

//                設定api訪問權限
//                前端要設定訪問權限 : 登入拿到token放到緩存 ,   設定某些網頁都要檢查token , token有帶身分 , buy
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 💡 權限檢查：現在這裡會正常運作了
                        .requestMatchers("/api/stores/**").hasAnyAuthority("STORES", "ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 💡 關鍵：把你的 JwtFilter 放在 Spring Security 原生檢查 Filter 之前
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

//    permitAll(): 門戶大開，不檢查身分（用於登入、註冊、查看公開菜單）。
//
//    hasAuthority("角色名"): 精確比對。你在 Token 裡塞 "STORES"，這裡就寫 "STORES"。
//
//    hasAnyAuthority("角色1", "角色2"): 多選一。只要符合其中一個角色就能進入。
//
//    authenticated(): 不限角色，只要 Token 是有效的（有登入）就可以。

    // 💡 簡單的 CORS 配置，允許前端 5173 埠存取
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // ❌ 絕對不能寫 config.addAllowedOrigin("*");
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "https://karan-nonsequacious-karina.ngrok-free.dev"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}