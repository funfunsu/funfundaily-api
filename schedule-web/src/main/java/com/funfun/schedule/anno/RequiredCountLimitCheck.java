package com.funfun.schedule.anno; // 放在合适的包下

import com.funfun.schedule.dto.BaseGroupRequest;
import com.funfun.schedule.enums.FunEntity;
import com.funfun.schedule.enums.GroupRole;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，用于标记需要进行数据权限检查的 Controller 方法。
 *
 * AOP 切面会拦截带有此注解的方法，并执行以下操作：
 * 1. 从安全上下文中获取当前用户 ID。
 * 2. 查找方法参数中继承自 {@link BaseGroupRequest} 且被 {@link RequestBody} 标记的对象。
 * 3. 如果找到，且其 {@code targetUserId} 字段为空，则自动填充为当前用户 ID。
 * 4. (可选) 可以在此处或委托给 Service 进行更细粒度的权限校验。
 *
 * 这确保了在进入业务逻辑之前，相关的请求对象已经携带了经过验证的用户身份信息，
 * 便于后续的业务层进行统一的数据访问控制。
 */
@Target(ElementType.METHOD) // 注解作用于方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，以便 AOP 能获取到
public @interface RequiredCountLimitCheck {
    FunEntity entity();
}