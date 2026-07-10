package com.orientsec.idap.common.model;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 前端提交的制度查询请求(frontend.regquery.v1.yaml QuerySubmit)。 */
public class QuerySubmit {

    @NotBlank
    @Size(max = 2000)
    private String question;

    private String sessionId;
    private List<String> attachmentFileIds;
    private Options options;

    public QuerySubmit() {}

    public QuerySubmit(
            String question, String sessionId, List<String> attachmentFileIds, Options options) {
        this.question = question;
        this.sessionId = sessionId;
        this.attachmentFileIds = attachmentFileIds;
        this.options = options;
    }

    public String question() {
        return question;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String sessionId() {
        return sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<String> attachmentFileIds() {
        return attachmentFileIds;
    }

    public List<String> getAttachmentFileIds() {
        return attachmentFileIds;
    }

    public void setAttachmentFileIds(List<String> attachmentFileIds) {
        this.attachmentFileIds = attachmentFileIds;
    }

    public Options options() {
        return options;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public static class Options {
        private Boolean includeSuperseded;

        public Options() {}

        public Options(Boolean includeSuperseded) {
            this.includeSuperseded = includeSuperseded;
        }

        public Boolean includeSuperseded() {
            return includeSuperseded;
        }

        public Boolean getIncludeSuperseded() {
            return includeSuperseded;
        }

        public void setIncludeSuperseded(Boolean includeSuperseded) {
            this.includeSuperseded = includeSuperseded;
        }
    }
}
