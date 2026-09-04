package com.idb.auth.common.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.util.StringUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Real OTP email delivery via Brevo's transactional email API. Replaces
 * {@link LoggingMailService} once {@code mail.provider=brevo} is set; the
 * logging stub stays the safe default otherwise.
 *
 * <p>No template engine exists in this project, so the one template this
 * project actually uses ({@code "otp"}) is rendered inline here rather than
 * pulling in Thymeleaf/FreeMarker for a single email.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "mail.provider", havingValue = "brevo")
public class BrevoMailService implements MailService {

    private static final String SEND_PATH = "/v3/smtp/email";

    private final RestClient restClient;
    private final String senderEmail;
    private final String senderName;

    public BrevoMailService(RestClient.Builder restClientBuilder,
            @Value("${brevo.api.base-url}") String baseUrl,
            @Value("${brevo.api.key}") String apiKey,
            @Value("${brevo.sender.email}") String senderEmail,
            @Value("${brevo.sender.name}") String senderName) {
        if (StringUtil.isBlank(apiKey) || StringUtil.isBlank(senderEmail)) {
            throw new IllegalStateException(
                    "mail.provider=brevo requires brevo.api.key and brevo.sender.email to be set");
        }
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void sendTemplatedEmail(MailInfo mailInfo) throws TraceableException {
        String htmlContent = renderHtml(mailInfo);
        try {
            restClient.post()
                    .uri(SEND_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildRequestBody(mailInfo, htmlContent))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw TraceableException.of("Brevo email delivery failed for %s", e, "Mail delivery failed",
                    mailInfo.getTo());
        }
    }

    private String renderHtml(MailInfo mailInfo) throws TraceableException {
        if ("otp".equals(mailInfo.getTemplateName())) {
            return renderOtpHtml(mailInfo.getTemplateModel());
        }
        if (!StringUtil.isBlank(mailInfo.getText())) {
            return mailInfo.getText();
        }
        throw TraceableException.of("Unsupported mail template '%s'",
                new IllegalStateException("no renderer for template"), "Mail delivery failed",
                mailInfo.getTemplateName());
    }

    private String renderOtpHtml(Map<String, Object> model) {
        String name = escape(model == null ? null : model.get("name"));
        String otp = escape(model == null ? null : model.get("otp"));
        String expiryMinutes = escape(model == null ? null : model.get("expiryMinutes"));
        return "<p>Hi " + name + ",</p>"
                + "<p>Your verification code is: <b>" + otp + "</b></p>"
                + "<p>This code expires in " + expiryMinutes + " minutes. "
                + "If you did not request this, you can ignore this email.</p>";
    }

    private String escape(Object value) {
        String text = String.valueOf(value);
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private Map<String, Object> buildRequestBody(MailInfo mailInfo, String htmlContent) {
        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("email", senderEmail);
        sender.put("name", senderName);

        List<Map<String, String>> recipients = mailInfo.getTo().stream()
                .map(email -> Map.of("email", email))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", sender);
        body.put("to", recipients);
        body.put("subject", mailInfo.getSubject());
        body.put("htmlContent", htmlContent);
        return body;
    }
}
