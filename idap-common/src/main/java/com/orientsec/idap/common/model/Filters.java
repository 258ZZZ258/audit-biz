package com.orientsec.idap.common.model;

import java.util.List;

/** 预计算的检索前置过滤值(Java 侧算,下传 audit-ai 边界当普通字段;§7 边界二 / boundary.v1.yaml Filters)。 */
public class Filters {

    private final List<String> permTags;
    private final List<String> corpusTypes;
    private final String projectId;
    private final String owner;

    public Filters(
            List<String> permTags, List<String> corpusTypes, String projectId, String owner) {
        this.permTags = permTags;
        this.corpusTypes = corpusTypes;
        this.projectId = projectId;
        this.owner = owner;
    }

    public List<String> permTags() {
        return permTags;
    }

    public List<String> getPermTags() {
        return permTags;
    }

    public List<String> corpusTypes() {
        return corpusTypes;
    }

    public List<String> getCorpusTypes() {
        return corpusTypes;
    }

    public String projectId() {
        return projectId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String owner() {
        return owner;
    }

    public String getOwner() {
        return owner;
    }
}
