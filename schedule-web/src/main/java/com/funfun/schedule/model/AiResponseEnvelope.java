package com.funfun.schedule.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 面向「微信小程序 AI 开发模式」的统一返回信封。
 *
 * 设计依据：微信官方建议接口返回采用「事实 + 动作」两段式（注意力权重最高），
 * 同时为前端原子组件提供结构化数据 (card)。
 *   - fact：已发生的事，给 AI 复述/确认（必填）
 *   - action：下一步可做什么，引导 AI 继续对话（可选，写操作建议带「撤销」选项）
 *   - card：原子组件渲染所需的结构化数据；type 决定用哪个原子组件
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiResponseEnvelope<T> {

    private String fact;
    private String action;
    private T card;

    public AiResponseEnvelope() {
    }

    public AiResponseEnvelope(String fact, String action, T card) {
        this.fact = fact;
        this.action = action;
        this.card = card;
    }

    public String getFact() {
        return fact;
    }

    public void setFact(String fact) {
        this.fact = fact;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public T getCard() {
        return card;
    }

    public void setCard(T card) {
        this.card = card;
    }

    public static <T> AiResponseEnvelope<T> of(String fact, String action, T card) {
        return new AiResponseEnvelope<>(fact, action, card);
    }
}
