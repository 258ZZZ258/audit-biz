package com.orientsec.idap.common.model;

/** 完整四级引用(§7.3 / §8.2 Java 收口):biz 按 chunk_id 回查 PG chunks⋈doc_versions 装配,交前端。 */
public class Citation {

    private final String chunkId;
    private final String clauseId;
    private final String docTitle;
    private final String docNo;
    private final String clausePath;
    private final Integer pageStart;
    private final Integer pageEnd;
    private final String version;
    private final String status;
    private final String snippet;
    private final Double matchScore;
    private final String theme;

    public Citation(
            String chunkId,
            String clauseId,
            String docTitle,
            String docNo,
            String clausePath,
            Integer pageStart,
            Integer pageEnd,
            String version,
            String status,
            String snippet,
            Double matchScore,
            String theme) {
        this.chunkId = chunkId;
        this.clauseId = clauseId;
        this.docTitle = docTitle;
        this.docNo = docNo;
        this.clausePath = clausePath;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.version = version;
        this.status = status;
        this.snippet = snippet;
        this.matchScore = matchScore;
        this.theme = theme;
    }

    public String chunkId() {
        return chunkId;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String clauseId() {
        return clauseId;
    }

    public String getClauseId() {
        return clauseId;
    }

    public String docTitle() {
        return docTitle;
    }

    public String getDocTitle() {
        return docTitle;
    }

    public String docNo() {
        return docNo;
    }

    public String getDocNo() {
        return docNo;
    }

    public String clausePath() {
        return clausePath;
    }

    public String getClausePath() {
        return clausePath;
    }

    public Integer pageStart() {
        return pageStart;
    }

    public Integer getPageStart() {
        return pageStart;
    }

    public Integer pageEnd() {
        return pageEnd;
    }

    public Integer getPageEnd() {
        return pageEnd;
    }

    public String version() {
        return version;
    }

    public String getVersion() {
        return version;
    }

    public String status() {
        return status;
    }

    public String getStatus() {
        return status;
    }

    public String snippet() {
        return snippet;
    }

    public String getSnippet() {
        return snippet;
    }

    public Double matchScore() {
        return matchScore;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public String theme() {
        return theme;
    }

    public String getTheme() {
        return theme;
    }
}
