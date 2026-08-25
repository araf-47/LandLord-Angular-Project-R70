package com.idb.auth.common.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MailInfo {
    private List<String> to;
    private String from;
    private String fromName;
    private String subject;
    private String text;
    private String templateName;
    private Map<String, Object> templateModel;
    private List<String> cc;
    private List<String> bcc;
}
