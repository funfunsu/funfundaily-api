package com.funfun.schedule.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "universal_record", indexes = {
        @Index(name = "idx_scene_business_key", columnList = "scene,business_key,scene_variables")
})
public class UniversalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene", nullable = false, length = 100)
    private String scene;

    @Column(name = "business_key", nullable = false, length = 127)
    private String businessKey;

    @Column(name = "scene_variables", length = 64) // 长度限制为 64
    private String sceneVariables; // 作为字符串存储

    @Column(name = "content", columnDefinition = "json")
    private String content; // 使用 String 存储 JSON

    @Column(name = "extra", columnDefinition = "json")
    private String extra; // 使用 String 存储 JSON

    @Column(name = "created_by", length = 100)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "updated_by", length = 100)
    private Long updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}