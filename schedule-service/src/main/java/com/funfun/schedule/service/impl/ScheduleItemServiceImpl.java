package com.funfun.schedule.service.impl;

import com.funfun.schedule.dto.ScheduleItemDTO;
import com.funfun.schedule.dto.ScheduleListItemDTO;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.ScheduleItemType;
import com.funfun.schedule.mapper.ScheduleItemMapper;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.service.ScheduleItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    public ScheduleItemServiceImpl(ScheduleItemRepository scheduleItemRepository, ScheduleItemMapper scheduleItemMapper) {
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

    private List<ScheduleItemDTO> getItemList(Long groupId , Long userId,String itemType){
        List<ScheduleItem> allScheduleItems = null;
        if (userId != null) {
            allScheduleItems = scheduleItemRepository.findByGroupIdAndUserId(groupId, userId);
        }else{
            allScheduleItems = scheduleItemRepository.findByGroupId(groupId);
        }
        return  scheduleItemMapper.toDTOList(allScheduleItems.stream().filter(scheduleItemDTO -> {return ScheduleItemType.task.name().equals(scheduleItemDTO.getItemType());}).collect(Collectors.toList()));
    }

    @Override
    public List<ScheduleListItemDTO> getTaskItemsByDateRange(Long groupId, Long userId, String fromDate, String toDate) {
        try {
            List<ScheduleItemDTO> allScheduleItemDTOS = getItemList(groupId,userId,ScheduleItemType.task.name());
            return transferToDateScheduleItems(fromDate,toDate,allScheduleItemDTOS);
        } catch (ParseException e) {
            throw new RuntimeException("Date format error: " + e.getMessage());
        }
    }

    @Override
    public List<ScheduleListItemDTO> getScheduleItemsByDateRange(Long groupId, Long userId, String fromDate, String toDate) {
        try {
            List<ScheduleItemDTO> allScheduleItemDTOS = getItemList(groupId,userId,ScheduleItemType.schedule.name());
            return transferToDateScheduleItems(fromDate,toDate,allScheduleItemDTOS);
        } catch (ParseException e) {
            throw new RuntimeException("Date format error: " + e.getMessage());
        }
    }


    @Override
    public List<ScheduleListItemDTO> transferToDateScheduleItems(String fromDate, String toDate,List<ScheduleItemDTO> list) throws ParseException {
        List<String> allDates = generateDates(fromDate, toDate);
        // 按日期分组存储结果
        List<ScheduleListItemDTO> result = new ArrayList<>(allDates.size());

        // 对每个日期进行处理
        for (String date : allDates) {
            // 过滤并填充该日期的日程项
            List<ScheduleItemDTO> filteredSchedules = filterAndFillSchedules(list, date);
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
    private List<ScheduleItemDTO> filterAndFillSchedules(List<ScheduleItemDTO> schedules, String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Date dateObj = dateFormat.parse(date);
        
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
        String monthlyRepeatKey = String.valueOf(dateObj.getDate());
        List<ScheduleItemDTO> monthlySchedules = schedules.stream()
                .filter(schedule -> "monthly".equals(schedule.getRepeatType()) && 
                        isDateWithinRepeatRange(schedule.getRepeatStartDay(), schedule.getRepeatEndDay(), dateObj) &&
                        containsRepeatKey(schedule.getRepeatKeys(), monthlyRepeatKey))
                .map(scheduleItemMapper::copy)
                .collect(Collectors.toList());
        
        // 每年重复日程项
        String yearlyRepeatKey = String.format("%d-%d", dateObj.getMonth() + 1, dateObj.getDate());
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
        
        return allSchedules;
    }
    
    /**
     * 检查日期是否在开始和结束时间范围内
     */
    private boolean isDateWithinRange(Date startTime, Date endTime, Date date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = dateFormat.format(date);
            Date startOfDay = dateFormat.parse(dateStr);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startOfDay);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Date endOfDay = calendar.getTime();
            
            return !startTime.after(endOfDay) && !endTime.before(startOfDay);
        } catch (ParseException e) {
            return false;
        }
    }
    
    /**
     * 检查日期是否在重复范围内
     */
    private boolean isDateWithinRepeatRange(Date repeatStartDay, Date repeatEndDay, Date date) {
        if (repeatStartDay == null) {
            return true;
        }
        
        if (repeatEndDay == null) {
            return !date.before(repeatStartDay);
        }
        
        return !date.before(repeatStartDay) && !date.after(repeatEndDay);
    }
    
    /**
     * 获取星期几（0-6，其中0表示星期日）
     */
    private int getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek - 1; // 转换为0-6，其中0表示星期日
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