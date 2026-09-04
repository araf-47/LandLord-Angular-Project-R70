package com.idb.auth.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Supplies the {@link RestClient.Builder} that
 * {@code com.idb.auth.common.service.BrevoMailService} needs. No
 * {@code RestClient.Builder} bean is autoconfigured in this project, so this
 * has to be provided explicitly - only registered when Brevo is actually the
 * active mail provider.
 */
@Configuration
@ConditionalOnProperty(name = "mail.provider", havingValue = "brevo")
public class BrevoConfig {

    @Bean
    public RestClient.Builder brevoRestClientBuilder() {
        return RestClient.builder();
    }
}
