package com.funfun.schedule.service;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.context.UserContext;
import com.funfun.schedule.dto.UniversalRecordDTO;
import com.funfun.schedule.entity.UniversalRecord;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.mapper.UniversalRecordMapper;
import com.funfun.schedule.repository.UniversalRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 通用记录服务层
 */
@Slf4j
@Service
public class UniversalRecordService {

    @Autowired
    private UniversalRecordRepository universalRecordRepository;

    @Autowired
    private UniversalRecordMapper mapper; // 注入 Mapper

    final static String SCENE_SYSTEM = "system";
    final static String DEFAULT_BIZ_KEY = "default";

    /**
     * 根据场景、场景变量和业务关键字查找具体数据 (返回 DTO)
     * @param sceneVar     dd
     * @param businessKey     业务关键字
     * @return Optional 包装的单个记录响应 DTO
     */
    public UniversalRecordDTO getRecord(String scene,String sceneVar,String businessKey) {
        Optional<UniversalRecord> entityOpt = universalRecordRepository.findBySceneAndSceneVarAndBusinessKey(scene, sceneVar, businessKey);
        if(entityOpt.isPresent()){
            return mapper.toDTO(entityOpt.get());
        }
        if (!DEFAULT_BIZ_KEY.equals(businessKey)) {
            log.info("加载默认配置{}{}",sceneVar,businessKey);
            entityOpt = universalRecordRepository.findBySceneAndSceneVarAndBusinessKey(SCENE_SYSTEM, sceneVar,DEFAULT_BIZ_KEY);
        }
        // 如果存在则转换为 DTO
        return entityOpt.map(universalRecord -> mapper.toDTO(universalRecord)).orElse(null);
    }
    /**
     * 根据场景、场景变量和业务关键字查找具体数据 (返回 DTO)
     * @param sceneVar     dd
     * @param businessKey     业务关键字
     * @return Optional 包装的单个记录响应 DTO
     */
    public UniversalRecordDTO getSystemConfigData(String sceneVar,String businessKey) {Optional<UniversalRecord> entityOpt = universalRecordRepository.findBySceneAndSceneVarAndBusinessKey(SCENE_SYSTEM, sceneVar, businessKey);
        UniversalRecordDTO recordDTO = getRecord(SCENE_SYSTEM,sceneVar,businessKey);
        if (recordDTO != null){
            return recordDTO;
        }
        CommonException.SERVER_ERROR.throwsError("配置不存在"+sceneVar);
        return null; // 如果存在则转换为 DTO
    }
    /**
     * 根据场景、场景变量和业务关键字查找具体数据 (返回 DTO)
     * @param sceneVar     业务关键字
     * @return Optional 包装的单个记录响应 DTO
     */
    public UniversalRecordDTO getSystemConfigData(String sceneVar) {
        return getSystemConfigData(sceneVar,DEFAULT_BIZ_KEY);
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
     * @param dto 包含更新数据的 DTO
     * @return Optional 包装的更新后的记录响应 DTO
     */
    public UniversalRecordDTO saveRecord(UniversalRecordDTO dto) {
        // 1. 根据 ID 查找现有记录
        Optional<UniversalRecord> existingEntityOpt = universalRecordRepository.findBySceneAndSceneVarAndBusinessKey(dto.getScene(),dto.getSceneVar(),dto.getBusinessKey());

        if (existingEntityOpt.isPresent()) {
            UniversalRecord existingEntity = existingEntityOpt.get();
            existingEntity.setContent(dto.getContent());
            existingEntity.setExtra(JSONObject.toJSONString(dto.getExtra()));
            existingEntity.setUpdatedBy(UserContext.getUserId());
            // 4. 保存更新后的 Entity
            UniversalRecord updatedEntity = universalRecordRepository.save(existingEntity);
            // 5. 将更新后的 Entity 转换为 Response DTO 并返回
            return mapper.toDTO(updatedEntity);
        } else {
            UniversalRecord universalRecord = mapper.toEntity(dto);
            universalRecord.setCreatedBy(UserContext.getUserId());
            universalRecord.setUpdatedBy(UserContext.getUserId());
            universalRecord  = universalRecordRepository.save(universalRecord);
            return mapper.toDTO(universalRecord);
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
    public List<UniversalRecordDTO> getRecords( String scene, String sceneVar) {
        List<UniversalRecord> entities = universalRecordRepository.findRangeBySceneAndSceneVar(scene,sceneVar);
        return mapper.toDTOList(entities);
    }
}