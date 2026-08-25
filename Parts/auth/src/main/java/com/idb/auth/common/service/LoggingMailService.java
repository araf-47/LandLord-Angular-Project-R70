package com.idb.auth.common.service;

import org.springframework.stereotype.Service;

import com.idb.auth.common.dto.MailInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * Default {@link MailService}: logs instead of sending. Replace with a real SMTP
 * implementation before using OTP login outside development.
 */
@Slf4j
@Service
public class LoggingMailService implements MailService {

    @Override
    public void sendTemplatedEmail(MailInfo mailInfo) {
        log.info("[MAIL:{}] to={} subject='{}'", mailInfo.getTemplateName(), mailInfo.getTo(), mailInfo.getSubject());
    }
}
