package com.example.demo.exception;

import com.example.demo.common.Result;
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
        return Result.error();
    }


//   預期中的業務錯誤（例如登入失敗）：
    @ExceptionHandler(CustomException.class)
    public Result error(CustomException e){
        e.printStackTrace();
        return Result.error(e.getCode(),e.getMsg());
//        自訂回傳
    }

    //密碼規則 攔截
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        System.out.println("成功進到攔截器了！");
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(message);
    }
}
