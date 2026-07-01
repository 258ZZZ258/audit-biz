package com.dfzq.auditai.biz.dto;

import java.util.List;

/**
 * 预计算的检索前置过滤值(Java 侧算,下传 audit-ai 边界当普通字段;§7 边界二 / boundary.v1.yaml Filters)。
 *
 * @param permTags 密级/职级标签清单(前置过滤位)
 * @param corpusTypes 可检索语料分区(internal/external/qa/case/audit_project)
 * @param projectId 项目隔离位;制度语料查询为 null
 * @param owner owner 行级 ABAC 过滤位——仅当 corpusTypes 含 audit_project 时非 null;制度语料不带(§7.x)
 */
public record Filters(
        List<String> permTags, List<String> corpusTypes, String projectId, String owner) {}
