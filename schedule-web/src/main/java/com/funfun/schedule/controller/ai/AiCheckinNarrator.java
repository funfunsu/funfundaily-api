package com.funfun.schedule.controller.ai;

import com.funfun.schedule.controller.ai.dto.AiCheckinCards;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 「微信 AI 打卡 SKILL」话术生成器。
 *
 * 集中管理面向 AI 的 fact / action 文案，遵循微信官方规范：
 *   - fact   ：先告诉 AI 发生了什么（注意力权重最高，必须包含关键事实）
 *   - action ：再告诉 AI 下一步能做什么（引导继续对话；写操作建议带回退路径）
 *
 * 文案要点：用"我"指代用户视角并保持口语；数字带单位；
 * 让 AI 容易复述给用户（避免技术黑话和 ID）。
 */
@Component
public class AiCheckinNarrator {

    private static final DateTimeFormatter DATE_HUMAN = DateTimeFormatter.ofPattern("M月d日");

    /** active-list 的 fact 文案。 */
    public String narrateActiveListFact(LocalDate date, AiCheckinCards.ActiveList card) {
        String dateText = humanDate(date);
        if (card.getPendingCount() == 0 && card.getCompletedCount() == 0) {
            return dateText + "没有打卡任务";
        }
        if (card.getPendingCount() == 0) {
            return dateText + "的 " + card.getCompletedCount() + " 个打卡任务已经全部完成";
        }
        if (card.getCompletedCount() == 0) {
            return dateText + "还有 " + card.getPendingCount() + " 个待打卡："
                    + joinTitles(card.getPending());
        }
        return dateText + "已完成 " + card.getCompletedCount() + " 个打卡，还有 "
                + card.getPendingCount() + " 个待打卡：" + joinTitles(card.getPending());
    }

    /** active-list 的 action 文案。 */
    public String narrateActiveListAction(AiCheckinCards.ActiveList card) {
        if (card.getPendingCount() == 0) {
            return null; // 都完成了，无需引导继续
        }
        if (card.getPendingCount() == 1) {
            return "回复『打卡 " + card.getPending().get(0).getTitle() + "』可以帮你完成";
        }
        return "回复任务名我可以帮你打卡，或者说『全部打卡』";
    }

    /** complete 的 fact 文案。 */
    public String narrateCompleteFact(AiCheckinCards.CompleteResult card) {
        StringBuilder sb = new StringBuilder("已为你完成『")
                .append(card.getTitle())
                .append("』")
                .append(humanDate(card.getDate()))
                .append("打卡");
        if (card.getCurrentStreak() > 1) {
            sb.append("，连续 ").append(card.getCurrentStreak()).append(" 天");
        }
        return sb.toString();
    }

    /** complete 的 action 文案：剩余引导 / 收尾鼓励。 */
    public String narrateCompleteAction(AiCheckinCards.CompleteResult card) {
        List<AiCheckinCards.TaskItem> remaining = card.getRemainingPending();
        if (remaining == null || remaining.isEmpty()) {
            return "今天的打卡都完成啦，干得漂亮 👏";
        }
        if (remaining.size() == 1) {
            return "还有『" + remaining.get(0).getTitle() + "』未打卡，要继续吗？";
        }
        return "还有 " + remaining.size() + " 个未打卡：" + joinTitles(remaining) + "，要继续吗？";
    }

    /** streak 的 fact 文案。 */
    public String narrateStreakFact(AiCheckinCards.Streak card) {
        StringBuilder sb = new StringBuilder("『").append(card.getTitle()).append("』");
        if (card.getCurrentStreak() <= 0) {
            sb.append("当前还没有连续打卡");
        } else {
            sb.append("当前连续 ").append(card.getCurrentStreak()).append(" 天");
        }
        sb.append("，本月已打卡 ").append(card.getMonthCheckinDays()).append(" 天");
        return sb.toString();
    }

    /** streak 的 action 文案。 */
    public String narrateStreakAction(AiCheckinCards.Streak card) {
        if (card.getCurrentStreak() <= 0) {
            return "今天还没打卡，要现在帮你打一下『" + card.getTitle() + "』吗？";
        }
        return "想看完整记录可以说『打开 " + card.getTitle() + " 的打卡日历』";
    }

    private static String humanDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        if (date.equals(LocalDate.now())) {
            return "今天";
        }
        if (date.equals(LocalDate.now().minusDays(1))) {
            return "昨天";
        }
        return date.format(DATE_HUMAN);
    }

    private static String joinTitles(List<AiCheckinCards.TaskItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(AiCheckinCards.TaskItem::getTitle)
                .collect(Collectors.joining("、"));
    }
}
