package com.orientsec.idap.core.controller;

import com.orientsec.idap.common.exception.NotImplementedException;
import com.orientsec.idap.common.model.CaseDetail;
import com.orientsec.idap.common.model.ClauseDetail;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import com.orientsec.idap.core.service.RegulationResourceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 前端制度查询契约中的资源明细与 v1 占位端点。 */
@RestController
public class RegulationResourceController {

    private final RegulationResourceService service;

    public RegulationResourceController(RegulationResourceService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/regulation/clauses/{clauseId}")
    public Result<ClauseDetail> clause(@PathVariable("clauseId") String clauseId) {
        return ResultGenerator.genSuccessResult(service.getClauseDetail(clauseId));
    }

    @GetMapping("/api/v1/regulation/cases/{caseId}")
    public Result<CaseDetail> caseDetail(@PathVariable("caseId") String caseId) {
        return ResultGenerator.genSuccessResult(service.getCaseDetail(caseId));
    }

    @GetMapping("/api/v1/regulation/sessions")
    public Result<Void> listSessions() {
        return placeholder("历史会话列表暂未实现");
    }

    @GetMapping("/api/v1/regulation/sessions/{sessionId}")
    public Result<Void> getSession(@PathVariable("sessionId") String sessionId) {
        return placeholder("历史会话详情暂未实现:" + sessionId);
    }

    @GetMapping("/api/v1/regulation/queries/{queryId}/audit-trail")
    public Result<Void> auditTrail(@PathVariable("queryId") String queryId) {
        return placeholder("操作与留痕暂未实现:" + queryId);
    }

    @GetMapping("/api/v1/regulation/queries/{queryId}/permission-trail")
    public Result<Void> permissionTrail(@PathVariable("queryId") String queryId) {
        return placeholder("权限轨迹暂未实现:" + queryId);
    }

    @PostMapping("/api/v1/regulation/queries/{queryId}/export")
    public Result<Void> export(@PathVariable("queryId") String queryId) {
        return placeholder("查询报告导出暂未实现:" + queryId);
    }

    @PostMapping("/api/v1/files")
    public Result<Void> uploadFile() {
        return placeholder("文件上传暂未实现");
    }

    private static <T> Result<T> placeholder(String message) {
        throw new NotImplementedException(message);
    }
}
