package com.funfun.schedule.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应类，用于包装所有controller的返回数据
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {
    
    /**
     * 响应码，0表示成功，其他表示失败
     */
    private int code = 0;
    
    /**
     * 响应消息
     */
    private String message = "success";
    
    /**
     * 响应数据
     */
    private T data;
    
    // 手动添加的getter和setter方法
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    /**
     * 成功响应，无数据
     */
    public static CommonResponse<Void> success() {
        CommonResponse<Void> response = new CommonResponse<>();
        response.code = 0;
        response.message = "success";
        return response;
    }
    
    /**
     * 成功响应，有数据
     */
    public static <T> CommonResponse<T> success(T data) {
        CommonResponse<T> response = new CommonResponse<>();
        response.code = 0;
        response.message = "success";
        response.data = data; // 直接设置字段值
        return response;
    }
    
    /**
     * 失败响应
     */
    public static CommonResponse<Void> fail(int code, String message) {
        CommonResponse<Void> response = new CommonResponse<>();
        response.code = code; // 直接设置字段值
        response.message = message; // 直接设置字段值
        return response;
    }
}