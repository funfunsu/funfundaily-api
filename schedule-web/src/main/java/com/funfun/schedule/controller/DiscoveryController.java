package com.funfun.schedule.controller;

import com.alibaba.fastjson2.JSON;
import com.funfun.schedule.dto.UniversalRecordDTO;
import com.funfun.schedule.enums.SystemConfigScene;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.UniversalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ScheduleGroupController类，提供群组相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    @Autowired
    private UniversalRecordService universalRecordService;

    @GetMapping("list")
    public CommonResponse<?> getList() {
        UniversalRecordDTO data = universalRecordService.getSystemConfigData(SystemConfigScene.DISCOVERY.getCode());
        return CommonResponse.success(JSON.parseArray(data.getContent()));
    }

}