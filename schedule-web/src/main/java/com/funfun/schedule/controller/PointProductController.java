package com.funfun.schedule.controller;

import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.point.CreatePointProductCommand;
import com.funfun.schedule.dto.point.PointProductDTO;
import com.funfun.schedule.dto.point.UpdatePointProductCommand;
import com.funfun.schedule.entity.PointProduct;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.PointProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 兑换商品控制器，提供商品管理的RESTful API接口。
 * 关联需求：Req-1（商品新增/编辑/删除）、Req-2（商品列表展示）、Req-5（边界隔离）
 */
@RestController
@RequestMapping("/api/point/product")
public class PointProductController {

    @Autowired
    private PointProductService pointProductService;

    /**
     * API-1: 获取群组兑换商品列表
     * 关联需求: Req-2, Req-5
     * 权限: 群组内所有成员可查看
     *
     * @param groupId 群组ID
     * @return 该群组内所有可兑换商品列表
     */
    @GetMapping("/list")
    public CommonResponse<List<PointProductDTO>> listProducts(@RequestParam Long groupId) {
        Long currentUserId = UserContext.getUserId();
        List<PointProduct> products = pointProductService.listActiveProducts(groupId, currentUserId);
        List<PointProductDTO> dtos = products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return CommonResponse.success(dtos);
    }

    /**
     * API-2: 新增兑换商品
     * 关联需求: Req-1
     * 权限: 仅群组的OWNER/ADMIN可新增
     *
     * @param request 新增商品请求
     * @return 新增商品的ID
     */
    @PostMapping("")
    public CommonResponse<CreateProductResponse> createProduct(@RequestBody CreateProductRequest request) {
        CreatePointProductCommand command = new CreatePointProductCommand();
        command.setGroupId(request.getGroupId());
        command.setName(request.getName());
        command.setDescription(request.getDescription());
        command.setRequiredScore(request.getRequiredScore());
        command.setOperator(UserContext.getUserId());

        PointProduct product = pointProductService.createProduct(command);
        return CommonResponse.success(new CreateProductResponse(product.getId()));
    }

    /**
     * API-3: 编辑兑换商品
     * 关联需求: Req-1
     * 权限: 仅群组的OWNER/ADMIN可编辑
     *
     * @param id      商品ID
     * @param request 编辑商品请求
     * @return 成功响应
     */
    @PutMapping("/{id}")
    public CommonResponse<Void> updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProductRequest request) {
        UpdatePointProductCommand command = new UpdatePointProductCommand();
        command.setId(id);
        command.setGroupId(request.getGroupId());
        command.setName(request.getName());
        command.setDescription(request.getDescription());
        command.setRequiredScore(request.getRequiredScore());
        command.setOperator(UserContext.getUserId());

        pointProductService.updateProduct(command);
        return CommonResponse.success();
    }

    /**
     * API-4: 移除兑换商品
     * 关联需求: Req-1, Req-5
     * 权限: 仅群组的OWNER/ADMIN可移除
     *
     * @param id      商品ID
     * @param groupId 群组ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResponse<Void> removeProduct(
            @PathVariable Long id,
            @RequestParam Long groupId) {
        Long currentUserId = UserContext.getUserId();
        pointProductService.removeProduct(id, groupId, currentUserId);
        return CommonResponse.success();
    }

    /**
     * 将商品实体转换为DTO用于API响应。
     */
    private PointProductDTO convertToDTO(PointProduct product) {
        return new PointProductDTO(
                product.getId(),
                product.getGroupId(),
                product.getName(),
                product.getDescription(),
                product.getRequiredScore(),
                product.getStatus()
        );
    }

    /**
     * 新增商品请求体
     */
    public static class CreateProductRequest {
        private Long groupId;
        private String name;
        private String description;
        private Integer requiredScore;

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
    }

    /**
     * 编辑商品请求体
     */
    public static class UpdateProductRequest {
        private Long groupId;
        private String name;
        private String description;
        private Integer requiredScore;

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
    }

    /**
     * 新增商品响应体
     */
    public static class CreateProductResponse {
        private Long id;

        public CreateProductResponse(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
