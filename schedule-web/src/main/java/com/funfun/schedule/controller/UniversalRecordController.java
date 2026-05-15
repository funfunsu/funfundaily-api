package com.funfun.schedule.controller;

import com.funfun.schedule.dto.UniversalRecordDTO;
import com.funfun.schedule.dto.schedule.GetUniversalRecordRequest;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.UniversalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 通用记录控制器
 */
@RestController
@RequestMapping("/api/universal-records")
public class UniversalRecordController {

    @Autowired
    private UniversalRecordService universalRecordService;

    /**
     * 根据场景和业务关键字查询记录 (范围查询)
     * GET /api/universal-records/range?scene=s1&businessKey=bk1
     */
    @PostMapping("/list")
    public CommonResponse<List<UniversalRecordDTO>> getRecordsBySceneAndBusinessKey(@RequestBody  GetUniversalRecordRequest request) {
        List<UniversalRecordDTO> records = universalRecordService.getRecords(request.getScene(), request.getSceneVariables());
        return CommonResponse.success(records);
    }

    /**
     * 根据场景、场景变量和业务关键字精确查找记录
     * GET /api/universal-records/exact?scene=s1&sceneVariables=sv1&businessKey=bk1
     * 注意：sceneVariables 参数现在是字符串，可能为 null
     */
    @PostMapping("/get")
    public CommonResponse<UniversalRecordDTO> getRecordBySceneVarsAndBusinessKey(@RequestBody GetUniversalRecordRequest request) {
        UniversalRecordDTO record = universalRecordService.getRecord(request.getScene(), request.getSceneVariables(), request.getBusinessKey());
        return CommonResponse.success(record);
    }

    /**
     * 创建新记录
     * POST /api/universal-records
     * 请求体应包含 JSON 格式的 UniversalRecordDTO 数据
     * @Valid 用于触发 DTO 的校验 (如果 DTO 中添加了校验注解)
     */
    @PostMapping
    public ResponseEntity<UniversalRecordDTO> createRecord(@RequestBody UniversalRecordDTO dto) {
        // 服务层处理创建逻辑并返回 DTO
        UniversalRecordDTO savedRecord = universalRecordService.createRecord(dto);
        // 返回 201 Created 状态码以及创建成功的数据
        return new ResponseEntity<>(savedRecord, HttpStatus.CREATED);
    }

    /**
     * 根据 ID 删除记录
     * DELETE /api/universal-records/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        Optional<UniversalRecordDTO> recordOpt = universalRecordService.getRecordById(id);
        if (recordOpt.isPresent()) {
            universalRecordService.deleteRecord(id);
            return ResponseEntity.noContent().build(); // 成功删除返回 204 No Content
        } else {
            return ResponseEntity.notFound().build(); // 记录不存在返回 404 Not Found
        }
    }
}