package com.orientsec.idap.common.model;

/** 条款原文 + 四级锚点 + 深链(frontend.regquery.v1.yaml ClauseDetail)。 */
public class ClauseDetail {

    private final String clauseId;
    private final String docTitle;
    private final String docNo;
    private final String clausePath;
    private final String fullText;
    private final Integer pageStart;
    private final Integer pageEnd;
    private final String version;
    private final String status;
    private final String deepLink;

    public ClauseDetail(
            String clauseId,
            String docTitle,
            String docNo,
            String clausePath,
            String fullText,
            Integer pageStart,
            Integer pageEnd,
            String version,
            String status,
            String deepLink) {
        this.clauseId = clauseId;
        this.docTitle = docTitle;
        this.docNo = docNo;
        this.clausePath = clausePath;
        this.fullText = fullText;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.version = version;
        this.status = status;
        this.deepLink = deepLink;
    }

    public String getClauseId() {
        return clauseId;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public String getDocNo() {
        return docNo;
    }

    public String getClausePath() {
        return clausePath;
    }

    public String getFullText() {
        return fullText;
    }

    public Integer getPageStart() {
        return pageStart;
    }

    public Integer getPageEnd() {
        return pageEnd;
    }

    public String getVersion() {
        return version;
    }

    public String getStatus() {
        return status;
    }

    public String getDeepLink() {
        return deepLink;
    }
}
