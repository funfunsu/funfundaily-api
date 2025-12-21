package com.funfun.schedule.aop;

import com.funfun.schedule.anno.RequiredDataPermission; // 导入新的注解
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.BaseGroupRequest;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.enums.GroupRole;
import com.funfun.schedule.enums.VipType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.service.GroupMemberService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(1) // 确保在大多数其他业务逻辑之前执行
public class DataPermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(DataPermissionAspect.class);

    @Autowired
    GroupMemberService groupMemberService;
    /**
     * 拦截所有被 @RequiredDataPermission 注解标记的 Controller 方法。
     *
     * @param joinPoint              切入点，提供对被拦截方法的访问。
     * @param requiredDataPermission 注解实例，可用于将来读取注解属性。
     * @return 原方法执行的结果。
     * @throws Throwable 如果被拦截的方法抛出异常，则原样抛出。
     */
    @Around("@annotation(requiredDataPermission)") // 使用 @annotation PCD 指定切入点
    public Object enforceDataPermission(ProceedingJoinPoint joinPoint, RequiredDataPermission requiredDataPermission) throws Throwable {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();

        // 1. 获取当前认证用户的 ID
        Long currentUserId = UserContext.getUserId();

        if (requiredDataPermission.onlyForVip()){
            VipType vipType = UserContext.getVipType();
            if (VipType.NONE.equals(vipType)){
                CommonException.ONLY_VIP_IS_ALLOWED.throwsError();
            }
        }

        // 2. 查找并处理 BaseGroupRequest 参数
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Annotation[] annotations = parameter.getAnnotations();

            // 检查参数是否被 @RequestBody 标记
            boolean hasRequestBody = false;
            for (Annotation annotation : annotations) {
                if (annotation instanceof RequestBody) {
                    hasRequestBody = true;
                    break;
                }
            }
            // 检查参数类型是否为 BaseGroupRequest (或其子类)
            if (hasRequestBody && args[i] instanceof BaseGroupRequest) {
                BaseGroupRequest baseRequest = (BaseGroupRequest) args[i];
                if (baseRequest.getGroupId() == null){
                    break;
                }
                UserContext.setOperateGroupId(Long.valueOf(baseRequest.getGroupId()));
                GroupMember gm = groupMemberService.getGroupMemberByGroupIdAndUserId(UserContext.getOperateGroupId(),currentUserId);
                if (GroupRole.Creator.name().equals(gm.getRole())){
                    break;
                }
                List<String> allowRoles = Arrays.stream(requiredDataPermission.allowRole()).map(Enum::name).collect(Collectors.toList());
                if (!allowRoles.contains(gm.getRole())){
                    CommonException.NOT_ALLOWED.throwsError("无权限");
                }
                break;
            } else if (hasRequestBody) {
                logger.trace("Parameter {} in method '{}' has @RequestBody but is not a BaseGroupRequest subclass. Skipped during @RequiredDataPermission processing.", i, method.getName());
            }
        }
        // 4. 继续执行原始的 Controller 方法
        return joinPoint.proceed(args);
    }
}