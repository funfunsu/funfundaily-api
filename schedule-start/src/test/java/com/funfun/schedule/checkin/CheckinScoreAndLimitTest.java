package com.funfun.schedule.checkin;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.service.CheckinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 任务积分玩法 + 每日打卡上限 的服务层集成测试（任务 #778）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>scoreMode=full（默认）：周期全部完成才得分；中途打卡不得分</li>
 *   <li>scoreMode=each：每次打卡都得分</li>
 *   <li>dailyLimit>=1：超过当天上限的打卡被拒绝</li>
 *   <li>历史数据（extra 无 dailyLimit）：不限制，行为不变</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CheckinScoreAndLimitTest {

    private static final Long GROUP_ID = 1L;
    private static final Long USER_ID = 1L;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private ScheduleItemRepository scheduleItemRepository;

    @Autowired
    private CheckinRecordRepository checkinRecordRepository;

    @BeforeEach
    void setUp() {
        scheduleItemRepository.deleteAll();
        checkinRecordRepository.deleteAll();
    }

    @Test
    void fullMode_awardsOnlyWhenPeriodCompleted() {
        // 每日习惯，次数=3，单次积分=10，full 模式
        Long taskId = createHabit("{\"taskType\":\"Habit\",\"totalCount\":3,\"score\":10,\"scoreMode\":\"full\",\"dailyLimit\":3}");

        assertEquals(0, awardedScore(checkin(taskId)), "第1次：未完成不得分");
        assertEquals(0, awardedScore(checkin(taskId)), "第2次：未完成不得分");
        assertEquals(10, awardedScore(checkin(taskId)), "第3次：完成得分");
    }

    @Test
    void eachMode_awardsEveryCheckin() {
        // 每日习惯，次数=3，单次积分=10，each 模式
        Long taskId = createHabit("{\"taskType\":\"Habit\",\"totalCount\":3,\"score\":10,\"scoreMode\":\"each\",\"dailyLimit\":3}");

        assertEquals(10, awardedScore(checkin(taskId)), "第1次即得分");
        assertEquals(10, awardedScore(checkin(taskId)), "第2次得分");
        assertEquals(10, awardedScore(checkin(taskId)), "第3次得分");
    }

    @Test
    void dailyLimit_blocksExtraCheckinSameDay() {
        // 周习惯，次数=5，每日上限=1：当天打卡第2次应被拒绝
        Long taskId = createHabit("weekly", "{\"taskType\":\"Habit\",\"totalCount\":5,\"score\":0,\"dailyLimit\":1}");

        checkin(taskId); // 第1次 OK
        MyException ex = assertThrows(MyException.class, () -> checkin(taskId), "第2次超过每日上限应抛错");
        assertEquals("4030000", ex.getCode(), "应为 NOT_ALLOWED");
    }

    @Test
    void noDailyLimit_legacyDataUnrestricted() {
        // 历史数据：extra 不含 dailyLimit -> 不限制
        Long taskId = createHabit("{\"taskType\":\"Habit\",\"totalCount\":3,\"score\":0}");

        checkin(taskId);
        checkin(taskId);
        checkin(taskId); // 不应抛错
        assertEquals(3, checkinRecordRepository
                .findByGroupIdAndUserIdAndTaskTimeBetween(GROUP_ID, USER_ID, taskId,
                        LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay())
                .size());
    }

    // ----- helpers -----

    private Long checkin(Long taskId) {
        CheckinRecordDTO dto = new CheckinRecordDTO();
        dto.setTaskId(taskId);
        dto.setGroupId(GROUP_ID);
        dto.setUserId(USER_ID);
        dto.setOperatorId(USER_ID);
        return checkinService.performCheckin(dto);
    }

    private int awardedScore(Long recordId) {
        String extra = checkinRecordRepository.findById(recordId).orElseThrow().getExtra();
        Integer score = JSONObject.parseObject(extra).getInteger("score");
        return score == null ? 0 : score;
    }

    private Long createHabit(String extraJson) {
        return createHabit("daily", extraJson);
    }

    private Long createHabit(String repeatType, String extraJson) {
        ScheduleItem item = new ScheduleItem();
        item.setItemTitle("习惯任务");
        item.setItemDesc("d");
        item.setItemType("task");
        item.setRepeatType(repeatType);
        item.setRepeatStartDay(LocalDate.now().minusDays(30));
        item.setRepeatEndDay(LocalDate.now().plusDays(30));
        item.setStartTime(LocalDateTime.now().minusDays(30));
        item.setEndTime(LocalDateTime.now().plusDays(30));
        item.setUserId(USER_ID);
        item.setGroupId(GROUP_ID);
        item.setParentId(0L);
        item.setCloseStatus(CloseStatus.OPEN);
        item.setExtra(extraJson);
        item.setCreateBy(USER_ID);
        item.setUpdateBy(USER_ID);
        item.setCreateTime(new Date());
        item.setUpdateTime(new Date());
        return scheduleItemRepository.save(item).getId();
    }
}
