package com.funfun.schedule.context;

import com.funfun.schedule.enums.VipType;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Long> OPERATE_GROUP_ID = new ThreadLocal<>();
    private static final ThreadLocal<VipType> USER_VIP_TYPE = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }
    public static void setOperateGroupId(Long groupId) {
        OPERATE_GROUP_ID.set(groupId);
    }

    public static Long getOperateGroupId() {
        return OPERATE_GROUP_ID.get();
    }
    public static void setVipType(VipType vipType) {
        USER_VIP_TYPE.set(vipType);
    }

    public static VipType getVipType() {
        return USER_VIP_TYPE.get();
    }

    public static void clear() {
        USER_ID.remove();
        OPERATE_GROUP_ID.remove();
        USER_VIP_TYPE.remove();
    }
}
