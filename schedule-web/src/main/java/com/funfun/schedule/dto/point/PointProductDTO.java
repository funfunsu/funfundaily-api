package com.funfun.schedule.dto.point;

/**
 * 兑换商品DTO，用于API响应。
 */
public class PointProductDTO {

    private Long id;
    private Long groupId;
    private String name;
    private String description;
    private Integer requiredScore;
    private String status;

    public PointProductDTO() {
    }

    public PointProductDTO(Long id, Long groupId, String name, String description, Integer requiredScore, String status) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.requiredScore = requiredScore;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRequiredScore() {
        return requiredScore;
    }

    public void setRequiredScore(Integer requiredScore) {
        this.requiredScore = requiredScore;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
