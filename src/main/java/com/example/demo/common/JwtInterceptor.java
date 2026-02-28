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

        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            throw new CustomException("401", "請先登入才能操作喔！");
        }

        try {
            // 💡 解析並取得 ID
            Long userId = jwtUtils.getUserIdFromToken(token);
            // 💡 關鍵：把 ID 存到 Request 裡，取名為 "currentUserId"
            request.setAttribute("currentUserId", userId);
        } catch (Exception e) {
            throw new CustomException("401", "Token 已過期或無效，請重新登入");
        }

        return true;
    }


}