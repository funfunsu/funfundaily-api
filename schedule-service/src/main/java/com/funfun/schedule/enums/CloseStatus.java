package com.funfun.schedule.enums;

import lombok.Getter;

@Getter
public enum CloseStatus {
    OPEN(0, "初始化"),
    CLOSE(2, "关闭");

    private final Integer code;
    private final String description;

    CloseStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CloseStatus fromCode(Integer code) {
        for (CloseStatus type : CloseStatus.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type code: " + code);
    }
}