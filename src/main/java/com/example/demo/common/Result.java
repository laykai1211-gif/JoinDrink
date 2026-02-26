package com.example.demo.common;

import lombok.Data;

@Data
public class Result {
    private String code;
    private String msg;
    private Object data;

    public static Result success(){
        Result result=new Result();
        result.setCode("200");
        result.setMsg("請求成功");
        return result;
    }

    public static Result success(Object data){
        Result result=success();
        result.setData(data);
        return result;
    }
//    //Result.success()：用於不需要回傳資料的操作（如：刪除成功、修改成功）。
//
//Result.success(Object data)：最常用的方法。當你查詢資料後，把結果塞進去回傳給前端。
//
//Result.error(String code, String msg)：當發生業務異常時（如：帳號已存在），自定義錯誤碼和訊息。
    public static Result error(){
        Result result = new Result();
        result.setCode("500");
        result.setMsg("系統錯誤");
        return result;
    }

    public static Result error(String code,String msg){
        Result result=new Result();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
    public static Result error(String msg) {
        Result result = new Result();
        result.setCode(""); // 或者你想要的預設錯誤碼
        result.setMsg(msg);
        return result;
    }


}
