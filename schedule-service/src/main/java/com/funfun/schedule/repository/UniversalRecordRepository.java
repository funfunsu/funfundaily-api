package com.funfun.schedule.repository;

import com.funfun.schedule.entity.UniversalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversalRecordRepository extends JpaRepository<UniversalRecord, Long> {

    /**
     * 根据场景和业务关键字查询范围数据
     * @param scene 场景
     * @param businessKey 业务关键字
     * @return 符合条件的记录列表
     */
    List<UniversalRecord> findBySceneAndBusinessKey(String scene, String businessKey);

    /**
     * 根据场景、场景变量和业务关键字查找具体数据
     * @param scene 场景
     * @param sceneVariables 场景变量 (字符串)
     * @param businessKey 业务关键字
     * @return Optional 包装的单个记录
     */
    Optional<UniversalRecord> findBySceneAndSceneVariablesAndBusinessKey(String scene, String sceneVariables, String businessKey);

    // --- 如果需要更灵活的查询，可以使用以下方法 ---

    /**
     * 使用 JPQL 根据场景和业务关键字查询
     * @param scene 场景
     * @param businessKey 业务关键字
     * @return 记录列表
     */
    @Query("SELECT ur FROM UniversalRecord ur WHERE ur.scene = :scene AND ur.businessKey = :businessKey")
    List<UniversalRecord> findRangeBySceneAndBusinessKey(@Param("scene") String scene, @Param("businessKey") String businessKey);

    /**
     * 使用 JPQL 根据场景、场景变量和业务关键字精确查找
     * @param scene 场景
     * @param sceneVariables 场景变量
     * @param businessKey 业务关键字
     * @return Optional 包装的单个记录
     */
    @Query("SELECT ur FROM UniversalRecord ur WHERE ur.scene = :scene AND ur.sceneVariables = :sceneVariables AND ur.businessKey = :businessKey")
    Optional<UniversalRecord> findExactBySceneVarsAndBusinessKey(@Param("scene") String scene, @Param("sceneVariables") String sceneVariables, @Param("businessKey") String businessKey);

    // 如果需要部分匹配 sceneVariables 或其他复杂查询，可以添加更多 @Query 方法
}