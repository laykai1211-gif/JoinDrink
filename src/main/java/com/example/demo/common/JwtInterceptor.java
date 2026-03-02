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
        // 1. 放行 OPTIONS 預檢請求
        if (request.getMethod().equals("OPTIONS")) return true;

        // 2. 💡 關鍵修正：放行所有 /api/auth/ 開頭的請求
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // 3. 剩下的請求（例如 /api/order, /api/user/profile）才檢查 Token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            throw new CustomException("401", "請先登入才能操作喔！");
        }

        try {
            Long userId = jwtUtils.getUserIdFromToken(token);
            request.setAttribute("currentUserId", userId);
        } catch (Exception e) {
            throw new CustomException("401", "Token 已過期或不合法，請重新登入");
        }

        return true;
    }
}