package com.dfzq.auditai.biz.dto;

/**
 * 前端提交的制度查询请求(frontend.regquery.v1.yaml QuerySubmit 的 A3 最小子集)。
 *
 * @param question 用户问题原文(必填)
 * @param sessionId 会话 id;null=新会话(A3 暂不落会话,预留)
 */
public record QuerySubmit(String question, String sessionId) {}
