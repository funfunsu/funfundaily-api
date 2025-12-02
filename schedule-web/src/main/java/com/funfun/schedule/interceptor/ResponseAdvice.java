//package com.funfun.schedule.interceptor;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.funfun.schedule.model.CommonResponse;
//import org.springframework.core.MethodParameter;
//import org.springframework.http.MediaType;
//import org.springframework.http.converter.HttpMessageConverter;
//import org.springframework.http.server.ServerHttpRequest;
//import org.springframework.http.server.ServerHttpResponse;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
//
///**
// * 响应拦截器，用于统一包装所有controller的返回数据到CommonResponse的data字段中
// */
//@RestControllerAdvice
//public class ResponseAdvice implements ResponseBodyAdvice<Object> {
//
//    private final ObjectMapper objectMapper;
//
//    public ResponseAdvice(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }
//
//    /**
//     * 判断是否需要拦截响应
//     */
//    @Override
//    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
//        // 对所有响应都进行拦截处理
//        return true;
//    }
//
//    /**
//     * 拦截处理响应数据，将其包装到CommonResponse的data字段中
//     */
//    @Override
//    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
//                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
//                                 ServerHttpRequest request, ServerHttpResponse response) {
//
//        // 如果响应体已经是CommonResponse类型，直接返回
//        if (body instanceof CommonResponse) {
//            return body;
//        }
//
//        // 处理String类型的特殊情况
//        if (body instanceof String) {
//            try {
//                CommonResponse<String> commonResponse = CommonResponse.success(body.toString());
//                return objectMapper.writeValueAsString(commonResponse);
//            } catch (JsonProcessingException e) {
//                // 转换失败时，返回原始字符串
//                return body;
//            }
//        }
//
//        // 将其他类型的数据包装到CommonResponse中
//        return CommonResponse.success(body);
//    }
//}