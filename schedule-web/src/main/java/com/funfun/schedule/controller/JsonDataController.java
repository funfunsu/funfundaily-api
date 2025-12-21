package com.funfun.schedule.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.anno.RequiredDataPermission;
import com.funfun.schedule.dto.schedule.GetJsonRequest;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.model.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/json-data") // 定义 API 基础路径
public class JsonDataController {

    // 注入配置的 JSON 数据基础路径
    @Value("${app.json.hanzi-writer-data.path}")
    private String hanziWriterPath;

    /**
     * 根据文件名获取 JSON 内容
     * 请求示例: GET /api/json-data/mydata.json
     */
    @PostMapping("/hanzi-writer/get") // .+ 允许文件名包含点号 (e.g., .json)
//    @RequiredDataPermission(onlyForVip = true)
    public CommonResponse<JSONObject> getJsonData(@RequestBody GetJsonRequest request) throws IOException {
        Path filePath = Paths.get(hanziWriterPath, request.getKey()+".json").normalize();

        // 基本的安全检查：防止路径遍历攻击 (可选但推荐)
        // 确保解析后的路径确实在 jsonDataPath 之内
        Path basePath = Paths.get(hanziWriterPath).toAbsolutePath().normalize();
        if (!filePath.startsWith(basePath)) {
            CommonException.NOT_ALLOWED.throwsError("Access denied.");
        }

        log.info("filePath:{}",filePath);

        // 检查文件是否存在
        if (!Files.exists(filePath)) {
            CommonException.NOT_FOUND.throwsError("File not found.");
        }

        // 读取文件内容为字符串
        String content = Files.readString(filePath);

        // 设置正确的 Content-Type 并返回内容
        return CommonResponse.success(JSON.parseObject(content));
    }
}