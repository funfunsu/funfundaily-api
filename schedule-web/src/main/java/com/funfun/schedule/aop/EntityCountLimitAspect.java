package com.funfun.schedule.aop;

import com.funfun.schedule.anno.RequiredCountLimitCheck;
import com.funfun.schedule.config.AppLimitConfig;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.enums.FunEntity;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.service.GroupMemberService;
import com.funfun.schedule.service.ScheduleGroupService;
import com.funfun.schedule.service.UserVipService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(2)
public class EntityCountLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(EntityCountLimitAspect.class);

    @Autowired
    GroupMemberService groupMemberService;
    @Autowired
    ScheduleGroupService groupService;
    @Autowired
    AppLimitConfig appLimitConfig;
    /**
     * 拦截所有被 @RequiredDataPermission 注解标记的 Controller 方法。
     *
     * @param joinPoint              切入点，提供对被拦截方法的访问。
     * @param requiredCountLimitCheck 注解实例，可用于将来读取注解属性。
     * @return 原方法执行的结果。
     * @throws Throwable 如果被拦截的方法抛出异常，则原样抛出。
     */
    @Around("@annotation(requiredCountLimitCheck)") // 使用 @annotation PCD 指定切入点
    public Object enforceDataPermission(ProceedingJoinPoint joinPoint, RequiredCountLimitCheck requiredCountLimitCheck) throws Throwable {
        Object[] args = joinPoint.getArgs();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 1. 获取当前认证用户的 ID
        Long currentUserId = UserContext.getUserId();
        FunEntity entity = requiredCountLimitCheck.entity();
        AppLimitConfig.Count countLimit = UserContext.getVipType() == null? appLimitConfig.getCount():appLimitConfig.getVipCount();
        int count = 0;
        switch (entity){
            case Group:
                count = groupService.involvedGroupCount(currentUserId);
                if (count >=countLimit.getGroup()){
                    CommonException.NOT_ALLOWED.throwsError("普通用户最多只建"+appLimitConfig.getCount().getGroup()+"个群组");
                }
            case GroupMember:
                Long groupId = UserContext.getOperateGroupId();
                count = groupMemberService.includeMemberCount(groupId);
                if (count >=countLimit.getGroupMember()){
                    CommonException.NOT_ALLOWED.throwsError("普通群不允许超过"+appLimitConfig.getCount().getGroupMember()+"个人");
                }

        }
        // 4. 继续执行原始的 Controller 方法
        return joinPoint.proceed(args);
    }
}