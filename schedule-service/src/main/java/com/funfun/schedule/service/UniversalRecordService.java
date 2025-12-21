package com.funfun.schedule.service;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.UniversalRecordDTO;
import com.funfun.schedule.entity.UniversalRecord;
import com.funfun.schedule.mapper.UniversalRecordMapper;
import com.funfun.schedule.repository.UniversalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通用记录服务层
 */
@Service
public class UniversalRecordService {

    @Autowired
    private UniversalRecordRepository universalRecordRepository;

    @Autowired
    private UniversalRecordMapper mapper; // 注入 Mapper

    /**
     * 根据场景和业务关键字查询范围数据 (返回 DTO)
     *
     * @param scene       场景
     * @param businessKey 业务关键字
     * @return 符合条件的记录响应 DTO 列表
     */
    public List<UniversalRecordDTO> getRecordsBySceneAndBusinessKey(String scene, String businessKey) {
        List<UniversalRecord> entities = universalRecordRepository.findBySceneAndBusinessKey(scene, businessKey);
        return entities.stream()
                .map(mapper::toDTO) // 转换为 DTO
                .collect(Collectors.toList());
    }

    /**
     * 根据场景、场景变量和业务关键字查找具体数据 (返回 DTO)
     *
     * @param scene           场景
     * @param sceneVariables  场景变量 (字符串)
     * @param businessKey     业务关键字
     * @return Optional 包装的单个记录响应 DTO
     */
    public Optional<UniversalRecordDTO> getRecordBySceneVarsAndBusinessKey(String scene, String sceneVariables, String businessKey) {
        Optional<UniversalRecord> entityOpt = universalRecordRepository.findBySceneAndSceneVariablesAndBusinessKey(scene, sceneVariables, businessKey);
        return entityOpt.map(mapper::toDTO); // 如果存在则转换为 DTO
    }

    /**
     * 保存或创建记录 (使用 DTO)
     *
     * @param dto 要保存的记录 DTO
     * @return 保存后的记录响应 DTO
     */
    public UniversalRecordDTO createRecord(UniversalRecordDTO dto) {
        // 1. 将 DTO 转换为 Entity (仅复制需要的字段)
        UniversalRecord entityToSave = mapper.toEntity(dto);
        // 2. (可选) 在这里可以根据安全上下文设置 createdBy, updatedBy
        // entityToSave.setCreatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        // entityToSave.setUpdatedBy(SecurityContextHolder.getContext().getAuthentication().getName());

        // 3. 保存到数据库
        UniversalRecord savedEntity = universalRecordRepository.save(entityToSave);
        // 4. 将保存后的 Entity 转换为 Response DTO 并返回
        return mapper.toDTO(savedEntity);
    }

    /**
     * 更新现有记录 (使用 DTO)
     *
     * @param id  要更新的记录 ID
     * @param dto 包含更新数据的 DTO
     * @return Optional 包装的更新后的记录响应 DTO
     */
    public Optional<UniversalRecordDTO> updateRecord(Long id, UniversalRecordDTO dto) {
        // 1. 根据 ID 查找现有记录
        Optional<UniversalRecord> existingEntityOpt = universalRecordRepository.findById(id);

        if (existingEntityOpt.isPresent()) {
            UniversalRecord existingEntity = existingEntityOpt.get();
            existingEntity.setContent(JSONObject.toJSONString(dto.getContent()));
            existingEntity.setExtra(JSONObject.toJSONString(dto.getExtra()));
            existingEntity.setUpdatedBy(UserContext.getUserId());
            // 4. 保存更新后的 Entity
            UniversalRecord updatedEntity = universalRecordRepository.save(existingEntity);

            // 5. 将更新后的 Entity 转换为 Response DTO 并返回
            return Optional.of(mapper.toDTO(updatedEntity));
        } else {
            // 记录不存在
            return Optional.empty();
        }
    }

    /**
     * 根据 ID 删除记录
     *
     * @param id 记录 ID
     */
    public void deleteRecord(Long id) {
        universalRecordRepository.deleteById(id);
    }

    /**
     * 根据 ID 获取记录 (返回 DTO)
     *
     * @param id 记录 ID
     * @return Optional 包装的记录响应 DTO
     */
    public Optional<UniversalRecordDTO> getRecordById(Long id) {
        Optional<UniversalRecord> entityOpt = universalRecordRepository.findById(id);
        return entityOpt.map(mapper::toDTO); // 如果存在则转换为 DTO
    }

    /**
     * 获取所有记录 (谨慎使用，大数据量时) (返回 DTO)
     *
     * @return 所有记录响应 DTO 列表
     */
    public List<UniversalRecordDTO> getAllRecords() {
        List<UniversalRecord> entities = universalRecordRepository.findAll();
        return entities.stream()
                .map(mapper::toDTO) // 转换为 DTO
                .collect(Collectors.toList());
    }
}