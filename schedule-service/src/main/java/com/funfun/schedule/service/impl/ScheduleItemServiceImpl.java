package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.QueryScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleItemUpdateScope;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.RepeatType;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.mapper.ScheduleItemMapper;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.service.ScheduleItemService;
import com.funfun.schedule.util.DateUtil;
import net.bytebuddy.asm.Advice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ScheduleItemService接口的实现类，实现ScheduleItem相关的业务逻辑
 */
@Service
public class ScheduleItemServiceImpl implements ScheduleItemService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final ScheduleItemMapper scheduleItemMapper;

    @Autowired
    public ScheduleItemServiceImpl(ScheduleItemRepository scheduleItemRepository,
                                   ScheduleItemMapper scheduleItemMapper) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.scheduleItemMapper = scheduleItemMapper;
    }

    @Override
    public boolean createScheduleItems(Long userId, Long groupId,Long targetUserId, List<ScheduleItemDTO> scheduleItem) {
        for (ScheduleItemDTO scheduleItemDTO : scheduleItem) {
            ScheduleItem scheduleItemEntity = scheduleItemMapper.toEntity(scheduleItemDTO);
            scheduleItemEntity.setUserId(targetUserId);
            scheduleItemEntity.setGroupId(groupId);
            scheduleItemEntity.setCreateBy(userId);
            scheduleItemEntity.setUpdateBy(userId);
            scheduleItemEntity.setCreateTime(new Date());
            scheduleItemEntity.setUpdateTime(new Date());

            if (scheduleItemEntity.getRepeatStartDay() == null || RepeatType.none.name().equals(scheduleItemEntity.getRepeatType())){
                scheduleItemEntity.setRepeatStartDay(DateUtil.getStartOfDay(scheduleItemEntity.getStartTime()));
            }

            if (scheduleItemEntity.getRepeatEndDay() == null || RepeatType.none.name().equals(scheduleItemEntity.getRepeatType())){
                scheduleItemEntity.setRepeatEndDay(DateUtil.getEndOfDay(scheduleItemEntity.getEndTime()));
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

    private List<ScheduleItemDTO> getItemList(Long groupId , Long userId,ScheduleItemType itemType,LocalDateTime fromDate, LocalDateTime toDate){
        List<ScheduleItem> allScheduleItems = null;
        if (userId == null){
            allScheduleItems = scheduleItemRepository.findOverlappingByGroupId(groupId,fromDate,toDate);
        }else{
            if (groupId == null){
                allScheduleItems = scheduleItemRepository.findOverlappingByUserId(userId,fromDate,toDate);
            }else{
                allScheduleItems = scheduleItemRepository.findOverlappingByGroupIdAndUserId(groupId, userId,fromDate,toDate);
            }
        }
        return  scheduleItemMapper.toDTOList(allScheduleItems.stream().filter(scheduleItemDTO -> {return itemType.name().equals(scheduleItemDTO.getItemType());}).collect(Collectors.toList()));
    }


    @Override
    public List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, LocalDateTime fromDate, LocalDateTime toDate,ScheduleItemType scheduleItemType) {
        try {
            // updateScope 由 mapper 从 schedule_item.update_scope 列直接反序列化
            List<ScheduleItemDTO> allScheduleItemDTOS = getItemList(groupId,userId,scheduleItemType,fromDate,toDate);
            return transferToDateScheduleItems(scheduleItemType, DateUtil.formatToLocalDateStr(fromDate),DateUtil.formatToLocalDateStr(toDate),allScheduleItemDTOS);
        } catch (ParseException e) {
            throw new RuntimeException("Date format error: " + e.getMessage());
        }
    }

    @Override
    public ScheduleItem saveForTaskUpdate(Long id, ScheduleItemUpdateScope updateScope) {
        ScheduleItem existing = scheduleItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ScheduleItem not found with id: " + id));
        existing.setUpdateScope(com.alibaba.fastjson2.JSON.toJSONString(updateScope));
        return scheduleItemRepository.save(existing);
    }

    @Override
    public List<ScheduleListItemDTO> transferToDateScheduleItems(String fromDate, String toDate,List<ScheduleItemDTO> list) throws ParseException {
        // 兼容旧调用，默认按 schedule 处理（不会算 dueDate，仅 itemKey）
        return transferToDateScheduleItems(ScheduleItemType.schedule, fromDate, toDate, list);
    }

    @Override
    public List<ScheduleListItemDTO> transferToDateScheduleItems(ScheduleItemType scheduleItemType, String fromDate, String toDate, List<ScheduleItemDTO> list) throws ParseException {
        List<String> allDates = generateDates(fromDate, toDate);
        List<ScheduleListItemDTO> result = new ArrayList<>(allDates.size());
        for (String date : allDates) {
            List<ScheduleItemDTO> filteredSchedules = filterAndFillSchedules(scheduleItemType, list, date);
            filteredSchedules.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
            result.add(new ScheduleListItemDTO(date, filteredSchedules));
        }
        return result;
    }

    @Override
    public List<ScheduleItemDTO> getItemList(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) return new ArrayList<>();
        List<ScheduleItem> items = scheduleItemRepository.findAllById(taskIds);
        return scheduleItemMapper.toDTOList(items);
    }

    @Override
    public List<ScheduleItemDTO> getItemListByParentIds(List<Long> parentIds) {
        // TODO: ScheduleItem 暂无 parentId 列 (entity/db 都缺)，等加上后改为 findByParentIdIn(parentIds)。
        // 现在保持空返回 + 日志，避免上层 NPE。
        if (parentIds != null && !parentIds.isEmpty()) {
            org.slf4j.LoggerFactory.getLogger(ScheduleItemServiceImpl.class)
                    .warn("getItemListByParentIds called but ScheduleItem.parentId not implemented yet; returning empty. parentIds={}", parentIds);
        }
        return new ArrayList<>();
    }

    @Override
    public List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, QueryScheduleItemDTO query) throws ParseException {
        List<ScheduleItemDTO> dtoList;
        if (query.getTaskIds() != null && !query.getTaskIds().isEmpty()) {
            dtoList = getItemList(query.getTaskIds());
        } else if (query.getParentIds() != null && !query.getParentIds().isEmpty()) {
            dtoList = getItemListByParentIds(query.getParentIds());
        } else {
            dtoList = getItemList(groupId, userId, query.getScheduleItemType(), query.getFromDate(), query.getToDate());
        }
        return transferToDateScheduleItems(
                query.getScheduleItemType(),
                DateUtil.formatToLocalDateStr(query.getFromDate()),
                DateUtil.formatToLocalDateStr(query.getToDate()),
                dtoList);
    }

    /**
     * 计算任务键 (与原始实现保持一致)：
     *   daily   → ${id}:${yyyy-MM-dd}
     *   weekly  → ${id}:${yyyy-Www}
     *   monthly → ${id}:${yyyy-MM}
     *   yearly  → ${id}:${yyyy}
     *   其他    → ${id}:        （none/未知）
     */
    public String getTaskKey(ScheduleItemDTO dto, LocalDate taskTime) {
        String key;
        String repeatType = dto.getRepeatType();
        if ("daily".equals(repeatType)) {
            key = taskTime.toString();
        } else if ("weekly".equals(repeatType)) {
            int weekOfYear = taskTime.get(WeekFields.ISO.weekOfYear());
            key = String.format("%d-W%02d", taskTime.getYear(), weekOfYear);
        } else if ("monthly".equals(repeatType)) {
            key = String.format("%d-%02d", taskTime.getYear(), taskTime.getMonthValue());
        } else if ("yearly".equals(repeatType)) {
            key = String.valueOf(taskTime.getYear());
        } else {
            key = "";
        }
        return dto.getId() + ":" + key;
    }

    /**
     * 计算到期日：
     *   daily   → 当日
     *   weekly  → 当周日
     *   monthly → 当月最后一天
     *   yearly  → 当年最后一天
     *   其他    → repeatEndDay 或 当日
     * 若结果晚于 repeatEndDay，截到 repeatEndDay。
     */
    public LocalDate getDueDate(ScheduleItemDTO dto, LocalDate taskTime) {
        LocalDate dueDate;
        String repeatType = dto.getRepeatType();
        if ("daily".equals(repeatType)) {
            dueDate = taskTime;
        } else if ("weekly".equals(repeatType)) {
            dueDate = taskTime.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } else if ("monthly".equals(repeatType)) {
            dueDate = taskTime.with(TemporalAdjusters.lastDayOfMonth());
        } else if ("yearly".equals(repeatType)) {
            dueDate = taskTime.with(TemporalAdjusters.lastDayOfYear());
        } else {
            LocalDateTime end = dto.getRepeatEndDay();
            dueDate = end != null ? end.toLocalDate() : taskTime;
        }
        if (dto.getRepeatEndDay() != null) {
            LocalDate repeatEnd = dto.getRepeatEndDay().toLocalDate();
            if (repeatEnd.isBefore(dueDate)) {
                return repeatEnd;
            }
        }
        return dueDate;
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
    private List<ScheduleItemDTO> filterAndFillSchedules(ScheduleItemType scheduleItemType, List<ScheduleItemDTO> schedules, String date) throws ParseException {
        LocalDateTime dateObj = DateUtil.parse(date);
        
        // 非重复日程项
        List<ScheduleItemDTO> noneRepeatSchedules = schedules.stream()
                .filter(schedule -> "none".equals(schedule.getRepeatType()) && 
                        isDateWithinRange(schedule.getStartTime(), schedule.getEndTime(), dateObj))
                .collect(Collectors.toList());
        
        // 每日重复日程项
        List<ScheduleItemDTO> dailySchedules = schedules.stream()
                .filter(schedule -> "daily".equals(schedule.getRepeatType()) && 
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());


        
        // 每周重复日程项
        String weeklyRepeatKey = String.valueOf(getDayOfWeek(dateObj));
        List<ScheduleItemDTO> weeklySchedules = schedules.stream()
                .filter(schedule -> "weekly".equals(schedule.getRepeatType()) && 
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), weeklyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        
        // 每月重复日程项
        String monthlyRepeatKey = String.valueOf(dateObj.getDayOfMonth());
        List<ScheduleItemDTO> monthlySchedules = schedules.stream()
                .filter(schedule -> "monthly".equals(schedule.getRepeatType()) && 
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), monthlyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        
        // 每年重复日程项
        String yearlyRepeatKey = String.format("%d-%d", dateObj.getMonthValue() + 1, dateObj.getDayOfMonth());
        List<ScheduleItemDTO> yearlySchedules = schedules.stream()
                .filter(schedule -> "yearly".equals(schedule.getRepeatType()) && 
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), yearlyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        
        // 合并所有日程项
        List<ScheduleItemDTO> allSchedules = new ArrayList<>();
        allSchedules.addAll(noneRepeatSchedules);
        allSchedules.addAll(dailySchedules);
        allSchedules.addAll(weeklySchedules);
        allSchedules.addAll(monthlySchedules);
        allSchedules.addAll(yearlySchedules);

        // 注入 showExtra：itemKey / dueDate(任务才算) / lastCompleteKey(有 updateScope 才算)
        LocalDate localDate = dateObj.toLocalDate();
        for (ScheduleItemDTO dto : allSchedules) {
            Map<String, Object> showExtra = new HashMap<>();
            showExtra.put("itemKey", getTaskKey(dto, localDate));
            if (ScheduleItemType.task.equals(scheduleItemType)) {
                showExtra.put("dueDate", getDueDate(dto, localDate).toString());
            }
            if (dto.getUpdateScope() != null && dto.getUpdateScope().getLastCompleteTime() != null) {
                LocalDate lastDay = dto.getUpdateScope().getLastCompleteTime().toLocalDate();
                showExtra.put("lastCompleteKey", getTaskKey(dto, lastDay));
            }
            dto.setShowExtra(showExtra);
        }

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
    private boolean isDateWithinRepeatRange(LocalDateTime repeatStartDay, LocalDateTime repeatEndDay, LocalDateTime date) {
        if (repeatStartDay == null) {
            return true;
        }
        
        if (repeatEndDay == null) {
            return !date.isBefore(repeatStartDay);
        }
        
        return !date.isBefore(repeatStartDay) && !date.isAfter(repeatEndDay);
    }
    private int getDayOfWeek(LocalDateTime date) {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek(); // 获取 DayOfWeek 枚举 (MONDAY=1, ..., SUNDAY=7)
        int isoValue = dayOfWeek.getValue();       // 获取 ISO 值 (1-7)
        return isoValue % 7;
    }


    /**
     * 检查重复键是否包含指定的键
     */
    private boolean containsRepeatKey(List<String> repeatKeys, String key) {
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