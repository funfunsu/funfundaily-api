
drop  table if exists universal_record;
CREATE TABLE universal_record (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  scene VARCHAR(100) NOT NULL COMMENT '场景标识',
                                  business_key VARCHAR(127) NOT NULL COMMENT '业务关键字',
                                  scene_variables VARCHAR(64)  COMMENT '场景变量',
                                  content JSON COMMENT '主要内容',
                                  extra JSON COMMENT '额外内容',
                                  created_by  bigint UNSIGNED  not null COMMENT '创建人',
                                  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  updated_by  bigint UNSIGNED  not null COMMENT '最后修改人',
                                  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
                                  INDEX idx_scene_business_key (scene, business_key,scene_variables)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用记录表';