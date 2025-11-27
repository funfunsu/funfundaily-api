package com.funfun.schedule.exception;

public class MyException extends RuntimeException {
    protected String code;

    public MyException(String errorCode) {
        this.code = errorCode;
    }

    public MyException(String errorCode, String message) {
        super(message);
        this.code = errorCode;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
