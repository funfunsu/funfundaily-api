package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.point.CreatePointProductCommand;
import com.funfun.schedule.dto.point.UpdatePointProductCommand;
import com.funfun.schedule.entity.GroupMember;
import com.funfun.schedule.entity.PointProduct;
import com.funfun.schedule.enums.PointProductStatus;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.GroupMemberRepository;
import com.funfun.schedule.repository.PointProductRepository;
import com.funfun.schedule.service.PointProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 兑换商品领域服务实现。
 */
@Service
public class PointProductServiceImpl implements PointProductService {

    @Autowired
    private PointProductRepository pointProductRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Override
    public List<PointProduct> listActiveProducts(Long groupId, Long requesterUserId) {
        validateGroupMember(groupId, requesterUserId);
        return pointProductRepository.findByGroupIdAndStatusOrderByIdDesc(groupId, PointProductStatus.ACTIVE.name());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointProduct createProduct(CreatePointProductCommand command) {
        validateCreateOrUpdate(command.getGroupId(), command.getName(), command.getRequiredScore(), command.getOperator());

        PointProduct product = new PointProduct();
        product.setGroupId(command.getGroupId());
        product.setName(command.getName().trim());
        product.setDescription(trimDescription(command.getDescription()));
        product.setRequiredScore(command.getRequiredScore());
        product.setStatus(PointProductStatus.ACTIVE.name());
        product.setCreatedBy(command.getOperator());
        product.setUpdatedBy(command.getOperator());
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());

        return pointProductRepository.save(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointProduct updateProduct(UpdatePointProductCommand command) {
        validateId(command.getId());
        validateCreateOrUpdate(command.getGroupId(), command.getName(), command.getRequiredScore(), command.getOperator());

        PointProduct product = pointProductRepository.findByIdAndGroupId(command.getId(), command.getGroupId())
                .orElseThrow(() -> new MyException("40401", "product not found"));

        if (PointProductStatus.REMOVED.name().equals(product.getStatus())) {
            throw new MyException("40002", "product unavailable");
        }

        product.setName(command.getName().trim());
        product.setDescription(trimDescription(command.getDescription()));
        product.setRequiredScore(command.getRequiredScore());
        product.setUpdatedBy(command.getOperator());
        product.setUpdateTime(new Date());

        return pointProductRepository.save(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeProduct(Long productId, Long groupId, Long operator) {
        validateId(productId);
        validateId(groupId);
        validateId(operator);
        validateAdminRole(groupId, operator);

        PointProduct product = pointProductRepository.findByIdAndGroupId(productId, groupId)
                .orElseThrow(() -> new MyException("40401", "product not found"));

        product.setStatus(PointProductStatus.REMOVED.name());
        product.setUpdatedBy(operator);
        product.setUpdateTime(new Date());

        pointProductRepository.save(product);
    }

    /**
     * 校验新增/编辑命令基础参数。
     */
    private void validateCreateOrUpdate(Long groupId, String name, Integer requiredScore, Long operator) {
        validateId(groupId);
        validateId(operator);
        if (name == null || name.trim().isEmpty() || name.trim().length() > 64) {
            throw new MyException("42201", "invalid request params");
        }
        if (requiredScore == null || requiredScore <= 0) {
            throw new MyException("42201", "invalid request params");
        }
        validateAdminRole(groupId, operator);
    }

    /**
     * 校验用户在群组中的管理权限。
     */
    private void validateAdminRole(Long groupId, Long userId) {
        GroupMember member = validateGroupMember(groupId, userId);
        String role = member.getRole();
        if (!isManagerRole(role)) {
            throw new MyException("40301", "permission denied");
        }
    }

    /**
     * 校验用户属于目标群组。
     */
    private GroupMember validateGroupMember(Long groupId, Long userId) {
        validateId(groupId);
        validateId(userId);
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new MyException("40001", "group not found"));
    }

    /**
     * 校验主键字段必须为正整数。
     */
    private void validateId(Long value) {
        if (value == null || value <= 0) {
            throw new MyException("42201", "invalid request params");
        }
    }

    /**
     * 角色识别：兼容 Creator/Admin 与 OWNER/ADMIN 语义。
     */
    private boolean isManagerRole(String role) {
        if (role == null) {
            return false;
        }
        return "ADMIN".equalsIgnoreCase(role)
                || "OWNER".equalsIgnoreCase(role)
                || "CREATOR".equalsIgnoreCase(role);
    }

    /**
     * 描述字段允许为空，若非空则限制长度。
     */
    private String trimDescription(String description) {
        if (description == null) {
            return null;
        }
        String value = description.trim();
        if (value.length() > 128) {
            throw new MyException("42201", "invalid request params");
        }
        return value;
    }
}
