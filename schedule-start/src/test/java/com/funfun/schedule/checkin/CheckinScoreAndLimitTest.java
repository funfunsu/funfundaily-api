package com.funfun.schedule.checkin;

import com.alibaba.fastjson2.JSONObject;
import com.funfun.schedule.dto.CheckinRecordDTO;
import com.funfun.schedule.entity.ScheduleItem;
import com.funfun.schedule.entity.TransactionFlow;
import com.funfun.schedule.enums.CloseStatus;
import com.funfun.schedule.enums.FlowType;
import com.funfun.schedule.exception.MyException;
import com.funfun.schedule.repository.CheckinRecordRepository;
import com.funfun.schedule.repository.ScheduleItemRepository;
import com.funfun.schedule.repository.TransactionFlowRepository;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Autowired
    private TransactionFlowRepository transactionFlowRepository;

    @BeforeEach
    void setUp() {
        scheduleItemRepository.deleteAll();
        checkinRecordRepository.deleteAll();
        transactionFlowRepository.deleteAll();
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

    @Test
    void eachMode_flowDescriptionIncludesCheckinSeq() {
        // 每日习惯，次数=3，单次10分，each 模式：每条流水备注「第N次打卡」
        Long taskId = createHabit("{\"taskType\":\"Habit\",\"totalCount\":3,\"score\":10,\"scoreMode\":\"each\",\"dailyLimit\":3}");
        checkin(taskId);
        checkin(taskId);
        checkin(taskId);

        List<TransactionFlow> flows = transactionFlowRepository
                .findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(GROUP_ID, USER_ID, FlowType.POINTS);
        // 3 条单次得分流水，备注分别含 第1/2/3 次打卡
        long seqFlows = flows.stream().filter(f -> f.getDescription().contains("第") && f.getDescription().contains("次打卡")).count();
        assertEquals(3, seqFlows, "每次打卡都应有一条带「第N次打卡」备注的流水");
        assertTrue(flows.stream().anyMatch(f -> f.getDescription().contains("第1次打卡")));
        assertTrue(flows.stream().anyMatch(f -> f.getDescription().contains("第3次打卡")));
    }

    @Test
    void completionBonus_awardedOnceWithRemark() {
        // 每日习惯，次数=3，单次5分(each)，周期全部完成额外奖励20分
        Long taskId = createHabit("{\"taskType\":\"Habit\",\"totalCount\":3,\"score\":5,\"scoreMode\":\"each\",\"dailyLimit\":3,\"bonusScore\":20}");
        checkin(taskId);
        checkin(taskId);
        Long lastRecord = checkin(taskId); // 第3次完成周期

        // 额外奖励流水：金额=20，备注含「周期内任务全部完成额外奖励」，且只发一次
        List<TransactionFlow> bonus = transactionFlowRepository
                .findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(GROUP_ID, USER_ID, FlowType.POINTS)
                .stream().filter(f -> f.getDescription().contains("周期内任务全部完成额外奖励")).toList();
        assertEquals(1, bonus.size(), "额外奖励仅在完成那一次发放一次");
        assertEquals(20, bonus.get(0).getAmount());
        // 完成那次的打卡记录 extra 记录 bonusScore
        assertEquals(20, JSONObject.parseObject(
                checkinRecordRepository.findById(lastRecord).orElseThrow().getExtra()).getInteger("bonusScore"));
    }

    @Test
    void fullMode_noBonusBeforeCompletion() {
        // full 模式 + 额外奖励：完成前既无单次分也无额外奖励
        Long taskId = createHabit("{\"taskType\":\"Habit\",\"totalCount\":2,\"score\":10,\"scoreMode\":\"full\",\"dailyLimit\":2,\"bonusScore\":50}");
        checkin(taskId); // 第1次，未完成
        assertEquals(0, transactionFlowRepository
                .findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(GROUP_ID, USER_ID, FlowType.POINTS).size(),
                "未完成前不应有任何积分流水");

        checkin(taskId); // 第2次完成：完成分10 + 额外奖励50
        List<TransactionFlow> flows = transactionFlowRepository
                .findByGroupIdAndUserIdAndFlowTypeOrderByCreatedAtDesc(GROUP_ID, USER_ID, FlowType.POINTS);
        assertEquals(2, flows.size(), "完成时发放：完成积分 + 额外奖励两条流水");
        assertEquals(60, flows.stream().mapToInt(TransactionFlow::getAmount).sum());
        assertTrue(flows.stream().anyMatch(f -> f.getDescription().contains("周期内任务全部完成额外奖励")));
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
