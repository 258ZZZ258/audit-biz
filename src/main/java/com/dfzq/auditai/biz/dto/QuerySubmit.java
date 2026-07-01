package com.dfzq.auditai.biz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 前端提交的制度查询请求(frontend.regquery.v1.yaml QuerySubmit 的 A3 最小子集)。
 *
 * <p>入口校验(§7 在 biz 边界校验):question 必填非空;失败经 GlobalErrorHandler 出 B2xx。
 *
 * @param question 用户问题原文(必填、非空、≤2000)
 * @param sessionId 会话 id;null=新会话(A3 用 queryId 作新会话 id,会话落库预留)
 */
public record QuerySubmit(@NotBlank @Size(max = 2000) String question, String sessionId) {}
