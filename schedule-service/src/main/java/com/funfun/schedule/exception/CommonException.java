package com.funfun.schedule.exception;

import java.text.MessageFormat;


public class CommonException {
    private String code;
    private String messagePattern;
    public static final CommonException OK = new CommonException("2000000", "成功");
    public static final CommonException PARAM_INVALID = new CommonException("4000000", "参数有误.{0}");
    public static final CommonException LOGIN_INVALID = new CommonException("4010001", "登录无效.{0}");
    public static final CommonException NOT_ALLOWED = new CommonException("4030000", "不允许操作.{0}");
    public static final CommonException DATA_DUPLICATE = new CommonException("4030001", "数据重复.{0}");
    public static final CommonException DATA_INVALID = new CommonException("4030002", "数据有误.{0}");
    public static final CommonException CONCURRENT_ERROR = new CommonException("4030004", "并发控制错误.{0}");
    public static final CommonException REDIS_CLIENT_ERROR = new CommonException("4030005", "redis异常,key={0}");
    public static final CommonException NOT_FOUND = new CommonException("4040000", "参数不存在.{0}");
    public static final CommonException DATA_NOT_EXIST = new CommonException("4040001", "数据不存在.");
    public static final CommonException USER_NOT_EXIST = new CommonException("4040002", "用户不存在.");
    public static final CommonException UPLOAD_OSS_ERROR = new CommonException("4040005", "文件上传失败.");
    public static final CommonException SERVER_ERROR = new CommonException("5000000", "服务器错误.{0}");

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessagePattern() {
        return this.messagePattern;
    }

    public void setMessagePattern(String messagePattern) {
        this.messagePattern = messagePattern;
    }

    public CommonException(String code, String messagePattern) {
        this.code = code;
        this.messagePattern = messagePattern;
    }

    public CommonException() {
    }

    public void throwsError(Object... args) throws MyException {
        if (args == null) {
            throw new MyException(this.getCode(), MessageFormat.format(this.getMessagePattern(), ""));
        } else {
            throw new MyException(this.getCode(), MessageFormat.format(this.getMessagePattern(), args));
        }
    }
}
