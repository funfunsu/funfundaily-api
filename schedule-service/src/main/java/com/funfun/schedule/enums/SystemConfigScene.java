package com.funfun.schedule.enums;

import lombok.Getter;

@Getter
public enum SystemConfigScene {
    DISCOVERY("discovery", "发现");

    private final String code;
    private final String description;

    SystemConfigScene(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SystemConfigScene fromCode(String code) {
        for (SystemConfigScene type : SystemConfigScene.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown flow type code: " + code);
    }
}
