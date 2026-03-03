package com.example.demo.exception;

import com.example.demo.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//處理全域異常
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 非預期錯誤 (例如：NullPointerException, 資料庫斷線)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleGeneralException(Exception e){
        e.printStackTrace(); // 保留控制台 Log 方便工程師除錯

        // 給前端看友善的訊息，不要洩漏底層代碼
        return ResponseEntity.status(500).body(Result.error("500", "系統目前忙碌中，請稍後再試"));
    }

    // 2. 預期的業務錯誤 (例如：號碼已註冊、Token 失效)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Result> handleCustomException(CustomException e){
        System.out.println("🎯 偵測到邏輯錯誤: " + e.getMessage());

        int status;
        try {
            // 防禦性寫法：如果 code 不是數字，則預設回傳 400
            status = Integer.parseInt(e.getCode());
        } catch (NumberFormatException nfe) {
            status = 400;
        }

        // 確保 status 在合法的 HTTP 範圍內
        if (status < 100 || status > 599) status = 400;

        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    // 3. JSR303 驗證錯誤 (例如：@NotBlank, @Size 失敗)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // 💡 關鍵：參數驗證失敗，通常回傳 400 Bad Request
        return ResponseEntity.status(400).body(Result.error("400", message));
    }
}