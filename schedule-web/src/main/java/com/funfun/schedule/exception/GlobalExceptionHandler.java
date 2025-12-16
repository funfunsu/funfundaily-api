package com.funfun.schedule.exception;

import com.funfun.schedule.model.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理类，确保所有异常都返回统一格式的响应
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Throwable.class)
    @ResponseBody
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        // 记录异常日志
        log.error("GlobalExceptionHandler",e);
        
        // 创建统一异常响应
        CommonResponse<Void> response = CommonResponse.fail(
                CommonException.SERVER_ERROR.getCode(),
                e.getMessage() != null ? e.getMessage() : "Internal Server Error"
        );
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(MyException.class)
    @ResponseBody
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(MyException e) {
        // 业务异常不需要记录完整堆栈
        log.error("BusinessException: " , e);
        
        // 创建统一业务异常响应
        CommonResponse<Void> response = CommonResponse.fail(e.getCode(), e.getMessage());
        
        // 根据异常的HTTP状态码返回对应的响应
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}