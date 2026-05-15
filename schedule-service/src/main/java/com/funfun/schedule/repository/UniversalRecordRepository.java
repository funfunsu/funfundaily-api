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
     * 根据场景、场景变量和业务关键字查找具体数据
     * @param scene 场景
     * @param SceneVar 场景变量 (字符串)
     * @param businessKey 业务关键字
     * @return Optional 包装的单个记录
     */
    Optional<UniversalRecord> findBySceneAndSceneVarAndBusinessKey(String scene, String SceneVar, String businessKey);

    // --- 如果需要更灵活的查询，可以使用以下方法 ---

    /**
     * 使用 JPQL 根据场景和业务关键字查询
     * @param scene 场景
     * @param sceneVar 业务关键字
     * @return 记录列表
     */
    @Query("SELECT ur FROM UniversalRecord ur WHERE ur.scene = :scene AND ur.sceneVar = :sceneVar")
    List<UniversalRecord> findRangeBySceneAndSceneVar(@Param("scene") String scene, @Param("sceneVar") String sceneVar);
}