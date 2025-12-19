package com.funfun.schedule.enums;

public enum VipType {
    NONE(0, "普通用户"),
    MONTHLY(1, "月度VIP"),
    ANNUAL(2, "年度VIP"),
    LIFETIME(3, "终身VIP");
    // 可以添加更多类型...

    private final int code;
    private final String description;

    VipType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 根据 code 获取枚举实例的便捷方法 (可选)
    public static VipType fromCode(int code) {
        for (VipType type : VipType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid VipType code: " + code);
    }
}
