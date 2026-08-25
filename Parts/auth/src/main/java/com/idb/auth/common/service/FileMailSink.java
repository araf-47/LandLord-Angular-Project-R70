package com.idb.auth.common.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Development mail catcher: writes each outgoing mail, <b>including the OTP</b>, as
 * a JSON file so a black-box API test can read it. The local equivalent of
 * pointing SMTP at MailHog.
 *
 * <p><b>Disabled unless {@code mail.sink.enabled=true}.</b> It must never be
 * enabled outside a local or CI environment: it writes one-time passwords to disk
 * in plaintext. The default {@link LoggingMailService} deliberately logs only the
 * recipient and template, never the code.
 *
 * <p>{@code @Primary} so that enabling the flag replaces the default sender
 * without the two beans becoming an ambiguity.
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "mail.sink.enabled", havingValue = "true")
public class FileMailSink implements MailService {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Value("${mail.sink.dir:./target/mail-sink}")
    private String sinkDir;

    @Override
    public void sendTemplatedEmail(MailInfo mailInfo) throws TraceableException {
        try {
            Path dir = Paths.get(sinkDir);
            Files.createDirectories(dir);
            // Ordered, collision-free filenames so a reader can pick the newest.
            Path file = dir.resolve(System.nanoTime() + "-" + mailInfo.getTemplateName() + ".json");
            Files.writeString(file, MAPPER.writeValueAsString(mailInfo),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            log.warn("[MAIL SINK] wrote {} - plaintext OTPs on disk, development use only", file);
        } catch (Exception e) {
            throw TraceableException.of("Failed to write mail to sink", e, "Mail delivery failed");
        }
    }
}
