package com.funfun.schedule.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.constants.TaskConstants;
import com.funfun.schedule.dto.QueryScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.enums.RepeatType;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.enums.TaskType;
import com.funfun.schedule.exception.CommonException;
import com.funfun.schedule.mapper.ScheduleItemMapper;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.scheduleitem.ScheduleItemDateRules;
import com.funfun.schedule.scheduleitem.ScheduleItemSupport;
import com.funfun.schedule.scheduleitem.ScheduleItemTypeHandler;
import com.funfun.schedule.scheduleitem.ScheduleItemTypeHandlerRegistry;
import com.funfun.schedule.service.ScheduleItemService;
import com.funfun.schedule.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

/**
 * ScheduleItemService接口的实现类，实现ScheduleItem相关的业务逻辑
 */
@Slf4j
@Service
public class ScheduleItemServiceImpl implements ScheduleItemService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleItemMapper scheduleItemMapper;
    private final ScheduleItemTypeHandlerRegistry typeHandlerRegistry;

    @Autowired
    public ScheduleItemServiceImpl(ScheduleItemRepository scheduleItemRepository,
                                   ScheduleItemMapper scheduleItemMapper,
                                   ScheduleItemTypeHandlerRegistry typeHandlerRegistry) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleItemMapper = scheduleItemMapper;
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    @Override
    public boolean createScheduleItems(Long userId, Long groupId,Long targetUserId, List<ScheduleItemDTO> scheduleItem) {
        for (ScheduleItemDTO scheduleItemDTO : scheduleItem) {
            if (scheduleItemDTO.getItemTitle() == null || scheduleItemDTO.getItemTitle().isBlank()){
                CommonException.DATA_INVALID.throwsError("标题不能为空");
            }
            ScheduleItem scheduleItemEntity = scheduleItemMapper.toEntity(scheduleItemDTO);
            scheduleItemEntity.setUserId(targetUserId);
            scheduleItemEntity.setGroupId(groupId);

            if (scheduleItemEntity.getRepeatStartDay() == null){
                scheduleItemEntity.setRepeatStartDay(DateUtil.getStartOfDay(LocalDateTime.now()).toLocalDate());
            }

            if (scheduleItemEntity.getRepeatEndDay() == null){
                scheduleItemEntity.setRepeatEndDay(DateUtil.getEndOfDay(LocalDateTime.now()).toLocalDate());
            }

            // 盖审计字段并兜底 NOT NULL 默认值（MapStruct.toEntity 可能用 DTO 的 null 覆盖实体初始值）。
            ScheduleItemSupport.stampCreate(scheduleItemEntity, userId);

            scheduleItemRepository.save(scheduleItemEntity);
        }
        return true;
    }

    @Override
    public ScheduleItemDTO getScheduleItemById(Long id) {
        Optional<ScheduleItem>  item =  scheduleItemRepository.findById(id);
        if (item.isPresent()){
            return scheduleItemMapper.toDTO(item.get());
        }
        return null;
    }

    @Override
    public String getTaskKey(ScheduleItemDTO scheduleItemDTO, LocalDate taskTime) {
        String key;
        switch (scheduleItemDTO.getRepeatType()) {
            case daily:
                // 格式化为 "yyyy-MM-dd"，例如 "2026-01-07"
                key = taskTime.toString();
                break;
            case weekly:
                // 年-周: 例如 "2026-W01"
                int weekOfYear = DateUtil.getWeekOfYear(taskTime); // 使用你提供的方法
                int year = taskTime.getYear();
                key = String.format("%d-W%02d", year, weekOfYear);
                break;
            case yearly:
                // 年: 例如 "2026"
                key = String.valueOf(taskTime.getYear());
                break;
            case monthly:
                // 年-月: 例如 "2026-01"
                int yearM = taskTime.getYear();
                int month = taskTime.getMonthValue();
                // 格式化月份为两位数，例如 01, 02, ... 12
                key = String.format("%d-%02d", yearM, month);
                break;
            default:
                key = "";
        }

        // 统一返回 taskId-key 的格式
        return scheduleItemDTO.getId() + ":" + key;

    }


    @Override
    public ScheduleItem saveForTaskUpdate(Long id, ScheduleItemUpdateScope updateScope) {
        Optional<ScheduleItem> existingItem = scheduleItemRepository.findById(id);
        if (existingItem.isPresent()) {
            ScheduleItem itemToUpdate = existingItem.get();
            itemToUpdate.setUpdateScope(JSON.toJSONString(updateScope));
            return scheduleItemRepository.save(itemToUpdate);
        } else {
            throw new RuntimeException("ScheduleItem not found with id: " + id);
        }
    }

    @Override
    public ScheduleItem updateCloseStatus(Long id, CloseStatus closeStatus) {
        Optional<ScheduleItem> existingItem = scheduleItemRepository.findById(id);
        if (!existingItem.isPresent()) {
            throw new RuntimeException("ScheduleItem not found with id: " + id);
        }
        ScheduleItem itemToUpdate = existingItem.get();
        itemToUpdate.setCloseStatus(closeStatus == null ? CloseStatus.OPEN : closeStatus);
        itemToUpdate.setUpdateTime(new Date());
        return scheduleItemRepository.save(itemToUpdate);
    }

    @Override
    public List<ScheduleItemDTO> getClosedItems(Long groupId, Long userId, ScheduleItemType itemType) {
        List<ScheduleItem> items = scheduleItemRepository
                .findByGroupIdAndUserIdAndItemTypeAndCloseStatus(groupId, userId, itemType.name(), CloseStatus.CLOSE);
        return scheduleItemMapper.toDTOList(items);
    }

    @Override
    public List<ScheduleItemDTO> getActiveItems(Long groupId, Long userId, ScheduleItemType itemType) {
        List<ScheduleItem> items = scheduleItemRepository
                .findByGroupIdAndUserIdAndItemTypeAndCloseStatus(groupId, userId, itemType.name(), CloseStatus.OPEN);
        return scheduleItemMapper.toDTOList(items);
    }

    @Override
    public ScheduleItem updateScheduleItem(Long id, ScheduleItem scheduleItem) {
        Optional<ScheduleItem> existingItem = scheduleItemRepository.findById(id);
        if (existingItem.isPresent()) {
            ScheduleItem itemToUpdate = existingItem.get();
            // 更新属性
            itemToUpdate.setItemTitle(scheduleItem.getItemTitle());
            itemToUpdate.setItemDesc(scheduleItem.getItemDesc());
            itemToUpdate.setLocation(scheduleItem.getLocation());
            itemToUpdate.setRepeatType(scheduleItem.getRepeatType());
            itemToUpdate.setRepeatKeys(scheduleItem.getRepeatKeys());
            itemToUpdate.setRepeatStartDay(scheduleItem.getRepeatStartDay());
            itemToUpdate.setRepeatEndDay(scheduleItem.getRepeatEndDay());
            itemToUpdate.setItemType(scheduleItem.getItemType());
            itemToUpdate.setStartTime(scheduleItem.getStartTime());
            itemToUpdate.setEndTime(scheduleItem.getEndTime());
            itemToUpdate.setUserId(scheduleItem.getUserId());
            itemToUpdate.setGroupId(scheduleItem.getGroupId());
            
            return scheduleItemRepository.save(itemToUpdate);
        } else {
            throw new RuntimeException("ScheduleItem not found with id: " + id);
        }
    }

    @Override
    public void deleteScheduleItem(Long id) {
        if (scheduleItemRepository.existsById(id)) {
            scheduleItemRepository.deleteById(id);
        } else {
            throw new RuntimeException("ScheduleItem not found with id: " + id);
        }
    }

    @Override
    public List<ScheduleItem> getScheduleItemsByGroupIdAndPersonId(Long groupId, Long personId) {
        return scheduleItemRepository.findByGroupIdAndUserId(groupId, personId);
    }

    @Override
    public List<ScheduleItem> getScheduleItemsByGroupId(Long groupId) {
        return scheduleItemRepository.findByGroupId(groupId);
    }

    @Override
    public List<ScheduleItem> getScheduleItemsByPersonId(Long personId) {
        return scheduleItemRepository.findByUserId(personId);
    }

    @Override
    public List<ScheduleItem> getScheduleItemsByItemType(String itemType) {
        return scheduleItemRepository.findByItemType(itemType);
    }

    @Override
    public List<ScheduleItem> getScheduleItemsByRepeatType(String repeatType) {
        return scheduleItemRepository.findByRepeatType(repeatType);
    }

    @Override
    @Transactional
    public List<ScheduleItem> batchCreateScheduleItems(List<ScheduleItem> scheduleItems) {
        return scheduleItemRepository.saveAll(scheduleItems);
    }

    @Override
    @Transactional
    public void batchDeleteScheduleItems(List<Long> ids) {
        for (Long id : ids) {
            if (!scheduleItemRepository.existsById(id)) {
                throw new RuntimeException("ScheduleItem not found with id: " + id);
            }
        }
        scheduleItemRepository.deleteAllById(ids);
    }

    private List<ScheduleItemDTO> getItemList(Long groupId , Long userId,ScheduleItemType itemType,LocalDate fromDate, LocalDate toDate){
        List<ScheduleItem> allScheduleItems = new ArrayList<>(
                fetchOverlapping(itemType.name(), groupId, userId, fromDate, toDate));

        // 关联类型（如「日程表」顺带展示「收到的邀请」invRecv）由对应 Handler 声明，service 负责拉取。
        for (ScheduleItemType companion : typeHandlerRegistry.get(itemType).companionTypes()) {
            allScheduleItems.addAll(fetchCompanion(companion.name(), groupId, userId, fromDate, toDate));
        }

        log.info("from db:{}{}{}{}{},list:{}",groupId,userId,itemType,fromDate,toDate,JSONObject.toJSONString(allScheduleItems));
        return  scheduleItemMapper.toDTOList(allScheduleItems);
    }

    /** 按 (groupId,userId) 组合拉取指定类型、与时间窗重叠的日程项。 */
    private List<ScheduleItem> fetchOverlapping(String itemType, Long groupId, Long userId, LocalDate fromDate, LocalDate toDate) {
        if (userId == null) {
            return scheduleItemRepository.findOverlappingByGroupId(itemType, groupId, fromDate, toDate);
        }
        if (groupId == null) {
            return scheduleItemRepository.findOverlappingByUserId(itemType, userId, fromDate, toDate);
        }
        return scheduleItemRepository.findOverlappingByGroupIdAndUserId(itemType, groupId, userId, fromDate, toDate);
    }

    /**
     * 拉取关联类型项（如 invRecv）：这类记录属于用户个人、可能来自其它群组，
     * 因此优先按 userId 拉取（忽略 groupId）；只有「整组所有成员」视图（userId 为空）才按 groupId。
     */
    private List<ScheduleItem> fetchCompanion(String itemType, Long groupId, Long userId, LocalDate fromDate, LocalDate toDate) {
        if (userId != null) {
            return scheduleItemRepository.findOverlappingByUserId(itemType, userId, fromDate, toDate);
        }
        if (groupId != null) {
            return scheduleItemRepository.findOverlappingByGroupId(itemType, groupId, fromDate, toDate);
        }
        return Collections.emptyList();
    }

    @Override
    public List<ScheduleItemDTO> getItemList(List<Long> taskIds) {
        List<ScheduleItem> allScheduleItems = scheduleItemRepository.findByIdIn(taskIds);
        return  scheduleItemMapper.toDTOList(allScheduleItems);
    }

    @Override
    public List<ScheduleItemDTO> getItemListByParentIds(List<Long> parentIds) {
        List<ScheduleItem> allScheduleItems = scheduleItemRepository.findByParentIdIn(parentIds);
        return  scheduleItemMapper.toDTOList(allScheduleItems);
    }

    @Override
    public List<ScheduleItemDTO> getPlanItems(Long groupId, ScheduleItemType itemType) {
        List<ScheduleItem> items = scheduleItemRepository
                .findByGroupIdAndItemTypeAndCloseStatusNot(groupId, itemType.name(), CloseStatus.CLOSE);
        return scheduleItemMapper.toDTOList(items);
    }

    @Override
    public List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, LocalDate fromDate, LocalDate toDate,ScheduleItemType scheduleItemType) {
        try {
            List<ScheduleItemDTO> allScheduleItemDTOS = getItemList(groupId,userId,scheduleItemType,fromDate,toDate);
            return transferToDateScheduleItems(scheduleItemType,fromDate.toString(),toDate.toString(),allScheduleItemDTOS);
        } catch (ParseException e) {
            throw new RuntimeException("Date format error: " + e.getMessage());
        }
    }

    @Override
    public List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, QueryScheduleItemDTO queryScheduleItemDTO) throws ParseException {
        List<ScheduleItemDTO> scheduleItemDTOList = null;
        if (queryScheduleItemDTO.getTaskIds() != null) {
            scheduleItemDTOList = getItemList(queryScheduleItemDTO.getTaskIds());
        } else if (queryScheduleItemDTO.getParentIds() != null) {
            scheduleItemDTOList = getItemListByParentIds(queryScheduleItemDTO.getParentIds());
        }else{
            scheduleItemDTOList = getItemList(groupId,userId,queryScheduleItemDTO.getScheduleItemType(),queryScheduleItemDTO.getFromDate(),queryScheduleItemDTO.getToDate());
        }
        return transferToDateScheduleItems(queryScheduleItemDTO.getScheduleItemType(),queryScheduleItemDTO.getFromDate().toString(),queryScheduleItemDTO.getToDate().toString(),scheduleItemDTOList);
    }

    @Override
    public List<ScheduleListItemDTO> transferToDateScheduleItems(ScheduleItemType scheduleItemType,String fromDate, String toDate,List<ScheduleItemDTO> list) throws ParseException {
        List<String> allDates = generateDates(fromDate, toDate);
        // 按日期分组存储结果
        List<ScheduleListItemDTO> result = new ArrayList<>(allDates.size());
        // 对每个日期进行处理
        for (String date : allDates) {
            // 过滤并填充该日期的日程项
            List<ScheduleItemDTO> filteredSchedules = filterAndFillSchedules(scheduleItemType,list, date);
            // 按开始时间排序
            filteredSchedules.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
            result.add(new ScheduleListItemDTO(date,filteredSchedules));
        }

        return result;
    }

    /**
     * 生成日期范围内的所有日期
     */
    private List<String> generateDates(String startDate, String endDate) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date start = dateFormat.parse(startDate);
        Date end = dateFormat.parse(endDate);

        long startMillis = start.getTime();
        long endMillis = end.getTime();
        
        if (startMillis > endMillis) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
        
        List<String> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        
        while (calendar.getTime().getTime() <= endMillis) {
            dates.add(dateFormat.format(calendar.getTime()));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        return dates;
    }
    
    /**
     * 根据日期过滤日程项，处理不同类型的重复规则
     */
    private List<ScheduleItemDTO> filterAndFillSchedules(ScheduleItemType scheduleItemType,List<ScheduleItemDTO> schedules, String date) throws ParseException {
        LocalDate dateObj = DateUtil.parse(date);
        ScheduleItemTypeHandler handler = typeHandlerRegistry.get(scheduleItemType);

        // 非重复日程项：是否出现由类型 Handler 判定（日程/邀请看时间窗，任务/目标/事件看重复区间）
        List<ScheduleItemDTO> noneRepeatSchedules = schedules.stream()
                .filter(schedule -> RepeatType.none.equals(schedule.getRepeatType()) &&
                        handler.occursOnNonRepeating(schedule, dateObj))
                .collect(Collectors.toList());
        
        // 每日重复日程项
        List<ScheduleItemDTO> dailySchedules = schedules.stream()
                .filter(schedule -> RepeatType.daily.equals(schedule.getRepeatType()) &&
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());


        
        // 每周重复日程项
        String weeklyRepeatKey = String.valueOf(getDayOfWeek(dateObj));
        List<ScheduleItemDTO> weeklySchedules = schedules.stream()
                .filter(schedule -> RepeatType.weekly.equals(schedule.getRepeatType()) &&
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), weeklyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        // 每单周重复日程项'oddWeek','evenWeek'
        List<ScheduleItemDTO> oddWeekSchedules = schedules.stream()
                .filter(schedule -> RepeatType.oddWeek.equals(schedule.getRepeatType()) &&
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        isOddWeek(schedule.getRepeatStartDay(),dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), weeklyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());


        // 每双周重复日程项
        List<ScheduleItemDTO> evenWeekSchedules = schedules.stream()
                .filter(schedule -> RepeatType.evenWeek.equals(schedule.getRepeatType()) &&
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        !isOddWeek(schedule.getRepeatStartDay(),dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), weeklyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());


        
        // 每月重复日程项
        String monthlyRepeatKey = String.valueOf(dateObj.getDayOfMonth());
        List<ScheduleItemDTO> monthlySchedules = schedules.stream()
                .filter(schedule -> RepeatType.monthly.equals(schedule.getRepeatType()) &&
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), monthlyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        
        // 每年重复日程项
        String yearlyRepeatKey = String.format("%d-%d", dateObj.getMonthValue() + 1, dateObj.getDayOfMonth());
        List<ScheduleItemDTO> yearlySchedules = schedules.stream()
                .filter(schedule -> RepeatType.yearly.equals(schedule.getRepeatType()) &&
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), yearlyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        
        // 合并所有日程项
        List<ScheduleItemDTO> allSchedules = new ArrayList<>();
        allSchedules.addAll(noneRepeatSchedules);
        allSchedules.addAll(dailySchedules);
        allSchedules.addAll(weeklySchedules);
        allSchedules.addAll(oddWeekSchedules);
        allSchedules.addAll(evenWeekSchedules);
        allSchedules.addAll(monthlySchedules);
        allSchedules.addAll(yearlySchedules);

        allSchedules.forEach(scheduleItemDTO -> {
            JSONObject showExtra = new JSONObject();
            showExtra.put("itemKey",getTaskKey(scheduleItemDTO,dateObj));
            // 类型特有的展示字段（如任务的 dueDate）交给对应 Handler 补充
            handler.decorate(scheduleItemDTO, dateObj, showExtra);
            if (scheduleItemDTO.getUpdateScope() != null && scheduleItemDTO.getUpdateScope().getLastCompleteTime()!= null){
                showExtra.put("lastCompleteKey",getTaskKey(scheduleItemDTO,scheduleItemDTO.getUpdateScope().getLastCompleteTime().toLocalDate()));
            }
            scheduleItemDTO.setShowExtra(showExtra);
        });
        
        return allSchedules;
    }
    
    /**
     * 检查日期是否在重复范围内（重复展开各分支共用，委托给共享规则）。
     */
    private boolean isDateWithinRepeatRange(LocalDate repeatStartDay, LocalDate repeatEndDay, LocalDate date) {
        return ScheduleItemDateRules.withinRepeatRange(repeatStartDay, repeatEndDay, date);
    }
    private int getDayOfWeek(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek(); // 获取 DayOfWeek 枚举 (MONDAY=1, ..., SUNDAY=7)
        int isoValue = dayOfWeek.getValue();       // 获取 ISO 值 (1-7)
        return isoValue % 7;
    }


    private boolean isOddWeek(LocalDate repeatStartDate, LocalDate curDate){
        if (repeatStartDate == null || curDate == null) {
            throw new IllegalArgumentException("Start date and current date cannot be null");
        }
        // 计算两个日期之间的天数差
        long daysBetween = ChronoUnit.DAYS.between(repeatStartDate, curDate);

        // 计算周数差 (整数部分)
        // 例如，如果相差 0-6 天，周数差为 0 (在同一周)；相差 7-13 天，周数差为 1 (下一周)。
        long weeksDifference = daysBetween / 7;

        // 判断奇偶性
        // repeatStartDate 被定义为单周 (Odd Week, 基数周)
        // 如果周数差是偶数，则 curDate 与 repeatStartDate 在相同类型的周 (奇 + 偶数 = 奇)
        // 如果周数差是奇数，则 curDate 与 repeatStartDate 在不同类型的周 (奇 + 奇数 = 偶)
        return (weeksDifference % 2 == 0);
    }


    /**
     * 检查重复键是否包含指定的键
     */
    private boolean containsRepeatKey(List<String> repeatKeys, String key) {
        if (repeatKeys.contains("whole")){
            return true;
        }
        if (repeatKeys == null || repeatKeys.isEmpty()) {
            return false;
        }
        boolean contains = repeatKeys.contains(key);
        if (!contains){
            contains = repeatKeys.contains("[\""+key+"\"");
        }
        if (!contains){
            contains = repeatKeys.contains("\""+key+"\"]");
        }
        if (!contains){
            contains = repeatKeys.contains("[\""+key+"\"]");
        }
        if (!contains){
            contains = repeatKeys.contains("\""+key+"\"");
        }
        return contains;
    }
}