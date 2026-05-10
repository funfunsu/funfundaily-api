package com.funfun.schedule.controller;

import com.funfun.schedule.entity.CheckinRecord;
import com.funfun.schedule.entity.ScoreFlow;
import com.funfun.schedule.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ScoreController类，提供积分相关的RESTful API接口
 */
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    @Autowired
    private ScoreService scoreService;

    /**
     * 用户完成任务后派发积分
     */
    @PostMapping("/distribute/task")
    public ResponseEntity<ScoreFlow> distributeScoreForTask(
            @RequestBody CheckinRecord checkinRecord,
            @RequestParam Integer score,
            @RequestParam Long operator,
            @RequestParam(required = false) Map<String, Object> extInfo) {
        ScoreFlow scoreFlow = scoreService.distributeScoreForTask(checkinRecord, score, operator, extInfo);
        return new ResponseEntity<>(scoreFlow, HttpStatus.CREATED);
    }

    /**
     * 积分兑换（减少积分）
     */
    @PostMapping("/redeem")
    public ResponseEntity<ScoreFlow> redeemScore(
            @RequestParam Long userId,
            @RequestParam Long groupId,
            @RequestParam Integer score,
            @RequestParam String eventName,
            @RequestParam Long operator,
            @RequestParam(required = false) Map<String, Object> extInfo) {
        ScoreFlow scoreFlow = scoreService.redeemScore(userId, groupId, score, eventName, operator, extInfo);
        return new ResponseEntity<>(scoreFlow, HttpStatus.CREATED);
    }

    /**
     * 查询用户的积分余额
     */
    @GetMapping("/balance")
    public ResponseEntity<Integer> getUserScoreBalance(
            @RequestParam Long userId,
            @RequestParam Long groupId) {
        Integer balance = scoreService.getUserScoreBalance(userId, groupId);
        return ResponseEntity.ok(balance);
    }

    /**
     * 查询用户的积分流水
     */
    @GetMapping("/flows")
    public ResponseEntity<List<ScoreFlow>> getUserScoreFlows(
            @RequestParam Long userId,
            @RequestParam Long groupId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<ScoreFlow> flows = scoreService.getUserScoreFlows(userId, groupId, startDate, endDate);
        return ResponseEntity.ok(flows);
    }

    /**
     * 根据ID查询积分流水
     */
    @GetMapping("/flows/{id}")
    public ResponseEntity<ScoreFlow> getScoreFlowById(@PathVariable Long id) {
        ScoreFlow flow = scoreService.getScoreFlowById(id);
        return ResponseEntity.ok(flow);
    }

    /**
     * 批量查询积分流水
     */
    @PostMapping("/flows/batch")
    public ResponseEntity<List<ScoreFlow>> getScoreFlowsByIds(@RequestBody List<Long> ids) {
        List<ScoreFlow> flows = scoreService.getScoreFlowsByIds(ids);
        return ResponseEntity.ok(flows);
    }

    /**
     * 检查用户积分是否足够
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkScoreSufficient(
            @RequestParam Long userId,
            @RequestParam Long groupId,
            @RequestParam Integer requiredScore) {
        boolean sufficient = scoreService.checkScoreSufficient(userId, groupId, requiredScore);
        return ResponseEntity.ok(sufficient);
    }
}