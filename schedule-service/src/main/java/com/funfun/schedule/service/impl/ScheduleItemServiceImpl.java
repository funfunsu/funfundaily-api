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
import java.time.temporal.TemporalAdjusters;
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

    @Autowired
    public ScheduleItemServiceImpl(ScheduleItemRepository scheduleItemRepository, ScheduleItemMapper scheduleItemMapper) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleItemMapper = scheduleItemMapper;
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
            scheduleItemEntity.setCreateBy(userId);
            scheduleItemEntity.setUpdateBy(userId);
            scheduleItemEntity.setCreateTime(new Date());
            scheduleItemEntity.setUpdateTime(new Date());

            if (scheduleItemEntity.getRepeatStartDay() == null){
                scheduleItemEntity.setRepeatStartDay(DateUtil.getStartOfDay(LocalDateTime.now()).toLocalDate());
            }

            if (scheduleItemEntity.getRepeatEndDay() == null){
                scheduleItemEntity.setRepeatEndDay(DateUtil.getEndOfDay(LocalDateTime.now()).toLocalDate());
            }

            // MapStruct.toEntity 会用 DTO 里 null 值覆盖字段初始化默认值，这里兜底，避免 NOT NULL 约束触发。
            if (scheduleItemEntity.getCloseStatus() == null) {
                scheduleItemEntity.setCloseStatus(CloseStatus.OPEN);
            }
            if (scheduleItemEntity.getParentId() == null) {
                scheduleItemEntity.setParentId(0L);
            }

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


    public LocalDate getDueDate(ScheduleItemDTO scheduleItemDTO, LocalDate taskTime) {
        LocalDate dueDate = null;

        switch (scheduleItemDTO.getRepeatType()) {
            case daily:
                // 每日重复：到期日就是 taskTime 所在的日期
                // 例如，taskTime 是 2024-03-15T10:30，则 dueDate 是 2024-03-15
                dueDate = taskTime;
                break;
            case weekly:
                // 每周重复：到期日是 taskTime 所在周的周日（一周的最后一天）
                // 例如，taskTime 是 2026-01-09 (周五)，则 dueDate 是 2026-01-11 (周日)
                dueDate = taskTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                break;
            case yearly:
                // 每年重复：到期日是 taskTime 所在年的最后一天，即12月31日
                // 例如，taskTime 是 2026-01-09T20:18，则 dueDate 是 2026-12-31
                dueDate = taskTime.with(TemporalAdjusters.lastDayOfYear());
                break;
            case monthly:
                // 每月重复：到期日是 taskTime 所在月份的最后一天
                // 例如，taskTime 是 2026-01-09T20:18，则 dueDate 是 2026-01-31
                // (如果是2月，会自动处理 2024-02-29 或 2023-02-28)
                dueDate = taskTime.with(TemporalAdjusters.lastDayOfMonth());
                break;
            default:
                // 如果传入了未定义的重复类型，使用 DTO 中指定的 endTime 的日期部分作为兜底
                dueDate = scheduleItemDTO.getRepeatEndDay();
        }
        boolean isRepeatEndBeforeDueDate = scheduleItemDTO.getRepeatEndDay().isBefore(dueDate);
        if (isRepeatEndBeforeDueDate){
            return scheduleItemDTO.getRepeatEndDay();
        }
        // 统一返回计算得出的到期日期
        return dueDate;
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
        List<ScheduleItem> allScheduleItems = null;
        if (userId == null){
            allScheduleItems = scheduleItemRepository.findOverlappingByGroupId(itemType.name(),groupId,fromDate,toDate);
        }else{
            if (groupId == null){
                allScheduleItems = scheduleItemRepository.findOverlappingByUserId(itemType.name(),userId,fromDate,toDate);
            }else{
                allScheduleItems = scheduleItemRepository.findOverlappingByGroupIdAndUserId(itemType.name(),groupId, userId,fromDate,toDate);
            }
        }

        if (allScheduleItems != null){
            log.info("from db:{}{}{}{}{},list:{}",groupId,userId,itemType,fromDate,toDate,JSONObject.toJSONString(allScheduleItems));
        }
        return  scheduleItemMapper.toDTOList(allScheduleItems);
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
        
        // 非重复日程项
        List<ScheduleItemDTO> noneRepeatSchedules = schedules.stream()
                .filter(schedule -> RepeatType.none.equals(schedule.getRepeatType()) &&
                        (ScheduleItemType.schedule.equals(scheduleItemType)?isDateWithinRange(schedule.getStartTime(), schedule.getEndTime(), dateObj.atStartOfDay()):isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj)))
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
            if(ScheduleItemType.task.equals(scheduleItemType)){
                LocalDate dueDate = getDueDate(scheduleItemDTO,dateObj);
                showExtra.put("dueDate",dueDate.toString());
            }
            if (scheduleItemDTO.getUpdateScope() != null && scheduleItemDTO.getUpdateScope().getLastCompleteTime()!= null){
                showExtra.put("lastCompleteKey",getTaskKey(scheduleItemDTO,scheduleItemDTO.getUpdateScope().getLastCompleteTime().toLocalDate()));
            }
            scheduleItemDTO.setShowExtra(showExtra);
        });
        
        return allSchedules;
    }
    
    /**
     * 检查日期是否在开始和结束时间范围内
     */
    private boolean isDateWithinRange(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime date) {
        // 1. 处理空值情况
        if (startTime == null || endTime == null || date == null) {
            return false;
        }
        LocalDateTime startOfDay = DateUtil.getStartOfDay(date);
        LocalDateTime endOfDay = DateUtil.getEndOfDay(date);
        return startTime.isAfter(startOfDay) && startTime.isBefore(endOfDay) || endTime.isAfter(startOfDay) && endTime.isBefore(endOfDay);
    }
    
    /**
     * 检查日期是否在重复范围内
     */
    private boolean isDateWithinRepeatRange(LocalDate repeatStartDay, LocalDate repeatEndDay, LocalDate date) {
        if (repeatStartDay == null) {
            return true;
        }
        
        if (repeatEndDay == null) {
            return !date.isBefore(repeatStartDay);
        }
        
        return !date.isBefore(repeatStartDay) && (date.isBefore(repeatEndDay) || date.equals(repeatEndDay));
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