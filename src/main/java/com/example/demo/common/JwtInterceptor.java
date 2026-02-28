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
        // 訪客路徑放行 (如果是 OPTIONS 也要放行，處理跨域)
        if (request.getMethod().equals("OPTIONS")) return true;

        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            throw new CustomException("401", "請先登入才能操作喔！");
        }

        // 這裡可以呼叫 jwtUtils 驗證 Token 是否過期或毀損
        return true;
    }
}