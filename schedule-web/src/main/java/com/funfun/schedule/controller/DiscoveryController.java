package com.funfun.schedule.controller;

import com.funfun.schedule.dto.DiscoveryItemDTO;
import com.funfun.schedule.dto.GroupDTO;
import com.funfun.schedule.model.CommonResponse;
import com.funfun.schedule.service.ScheduleGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ScheduleGroupController类，提供群组相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    @Autowired
    private ScheduleGroupService scheduleGroupService;

    /**
     *  curl -X POST http://localhost:8080/api/groups/create \
     *   -H "Content-Type: application/json" \
     *   -d '{"groupName": "VVvv"}'
     * 创建群组
     */
    @GetMapping("list")
    public CommonResponse getList() {
        List<DiscoveryItemDTO> list = new ArrayList<>();
        DiscoveryItemDTO itemDTO;
        itemDTO = new DiscoveryItemDTO();
        itemDTO.setItemTitle("积分管理");
        itemDTO.setUri("/pages/point/management");
        itemDTO.setItemType("path");
        list.add(itemDTO);
        itemDTO = new DiscoveryItemDTO();
        itemDTO.setItemTitle("积分兑换");
        itemDTO.setUri("/pages/point/exchange");
        itemDTO.setItemType("path");
        list.add(itemDTO);
        itemDTO = new DiscoveryItemDTO();
        itemDTO.setItemTitle("汉字书写");
        itemDTO.setUri("/subPackages/study-tools/pages/writing/stroke-order");
        itemDTO.setItemType("path");
        itemDTO.setStatus("active");
        list.add(itemDTO);
        return CommonResponse.success(list);
    }

}