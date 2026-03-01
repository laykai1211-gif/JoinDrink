package com.example.demo.exception;

import com.example.demo.common.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

//處理全域異常
@RestControllerAdvice
public class GlobalExceptionHandler {

//   非預期錯誤（例如程式碼寫錯）
    @ExceptionHandler(Exception.class)
    public Result error(Exception e){
        e.printStackTrace();
//        自訂回傳
        return Result.error("500", "伺服器發生意外： "+ e.getMessage());
    }


//   預期中的業務錯誤（例如登入失敗）：
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Result> error(CustomException e){
        // 💡 這裡會印出控制台訊息
        System.out.println("🎯 偵測到邏輯錯誤: " + e.getMessage());

        // 💡 關鍵：強制把 String code 轉成數字狀態碼 (如 401)
        int status = Integer.parseInt(e.getCode());

        // 💡 回傳 ResponseEntity，這會讓瀏覽器的 Network 變成「紅色」
        // 前端的 Axios 看到紅色，才會乖乖跑進 .catch()
        return ResponseEntity.status(status).body(Result.error(e.getCode(), e.getMessage()));
    }

    //密碼規則 攔截
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(message);
    }
}
