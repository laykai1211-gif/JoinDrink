package com.example.demo.common;

import com.example.demo.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod().equals("OPTIONS")) return true;
        System.out.println(">>> 進入攔截器，路徑：" + request.getRequestURI());
        String path = request.getRequestURI();

        // 💡 1. 只放行不需要登入的「白名單」
        // 登入、三方登入、註冊不需要 Token，其他的（如 apply-store）需要
        if (path.equals("/api/auth/login") ||
                path.equals("/api/auth/social-login") ||
                path.equals("/api/auth/register")) {
            return true;
        }

        // 💡 2. 統一從 Authorization 拿 Token (相容性最強)
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 去除 "Bearer " 前綴
        } else {
            // 如果你還是想用自定義的 "token" header 作為備案
            token = request.getHeader("token");
        }

        if (token == null || token.isEmpty()) {
            throw new CustomException("401", "請先登入才能操作喔！");
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            // 💡 3. 確保塞入屬性，Controller 才拿得到
            request.setAttribute("currentUserId", userId);
        } catch (Exception e) {
            throw new CustomException("401", "Token 無效，請重新登入");
        }

        return true;
    }
}