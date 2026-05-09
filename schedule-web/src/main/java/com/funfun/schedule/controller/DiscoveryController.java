package com.funfun.schedule.controller;

import com.funfun.schedule.config.DiscoveryConfig;
import com.funfun.schedule.dto.DiscoveryItemDTO;
import com.funfun.schedule.model.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 发现页入口列表，条目由 application.yml 的 app.discovery.items 驱动。
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    @Autowired
    private DiscoveryConfig discoveryConfig;

    @GetMapping("list")
    public CommonResponse<List<DiscoveryItemDTO>> getList() {
        List<DiscoveryItemDTO> items = discoveryConfig.getItems();
        return CommonResponse.success(items != null ? items : Collections.emptyList());
    }
}
