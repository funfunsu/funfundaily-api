package com.funfun.schedule.dto.schedule;

import com.funfun.schedule.dto.ScheduleItemDTO;

import java.util.List;

public class CreateScheduleItemRequest {
    public String groupId;
    List<ScheduleItemDTO> items;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<ScheduleItemDTO> getItems() {
        return items;
    }

    public void setItems(List<ScheduleItemDTO> items) {
        this.items = items;
    }
}
