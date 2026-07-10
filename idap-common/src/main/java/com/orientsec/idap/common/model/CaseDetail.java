package com.orientsec.idap.common.model;

/** 案例详情(frontend.regquery.v1.yaml CaseDetail)。 */
public class CaseDetail {

    private final String caseId;
    private final String caseName;
    private final String regulator;
    private final String penaltyDate;
    private final String violationTopic;
    private final String relatedRegulation;
    private final String coreIssue;
    private final String insight;
    private final String fullText;
    private final String sourceUrl;

    public CaseDetail(
            String caseId,
            String caseName,
            String regulator,
            String penaltyDate,
            String violationTopic,
            String relatedRegulation,
            String coreIssue,
            String insight,
            String fullText,
            String sourceUrl) {
        this.caseId = caseId;
        this.caseName = caseName;
        this.regulator = regulator;
        this.penaltyDate = penaltyDate;
        this.violationTopic = violationTopic;
        this.relatedRegulation = relatedRegulation;
        this.coreIssue = coreIssue;
        this.insight = insight;
        this.fullText = fullText;
        this.sourceUrl = sourceUrl;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getCaseName() {
        return caseName;
    }

    public String getRegulator() {
        return regulator;
    }

    public String getPenaltyDate() {
        return penaltyDate;
    }

    public String getViolationTopic() {
        return violationTopic;
    }

    public String getRelatedRegulation() {
        return relatedRegulation;
    }

    public String getCoreIssue() {
        return coreIssue;
    }

    public String getInsight() {
        return insight;
    }

    public String getFullText() {
        return fullText;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }
}
