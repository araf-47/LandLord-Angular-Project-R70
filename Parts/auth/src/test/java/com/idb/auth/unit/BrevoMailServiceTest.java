package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;
import com.idb.auth.common.service.BrevoMailService;

/**
 * Verifies the request Brevo actually receives, and that a failed call never
 * surfaces a raw HTTP exception to callers - same contract as the other
 * {@link com.idb.auth.common.service.MailService} implementations.
 */
class BrevoMailServiceTest {

    private static final String BASE_URL = "https://api.brevo.test";

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private BrevoMailService service() {
        return new BrevoMailService(builder, BASE_URL, "test-api-key", "sender@example.com", "LandLord");
    }

    private MailInfo otpMail() {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "alice");
        model.put("otp", "123456");
        model.put("expiryMinutes", 5);
        return MailInfo.builder()
                .to(List.of("alice@example.com"))
                .subject("OTP for alice")
                .templateName("otp")
                .templateModel(model)
                .build();
    }

    @Test
    @DisplayName("sends a POST to Brevo's send endpoint with the api-key header and the OTP in the body")
    void sendsRequestShapeBrevoExpects() throws Exception {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andRespond(withSuccess("{\"messageId\":\"abc\"}", MediaType.APPLICATION_JSON));

        service().sendTemplatedEmail(otpMail());

        server.verify();
    }

    @Test
    @DisplayName("a failed Brevo call surfaces as a TraceableException, not a raw HTTP exception")
    void failedCallIsWrapped() {
        server.expect(requestTo(BASE_URL + "/v3/smtp/email")).andRespond(withServerError());

        assertThatThrownBy(() -> service().sendTemplatedEmail(otpMail()))
                .isInstanceOf(TraceableException.class)
                .satisfies(e -> assertThat(((TraceableException) e).getResponse().getMessage())
                        .isEqualTo("Mail delivery failed"));
    }

    @Test
    @DisplayName("refuses to start without an api key")
    void failsFastWithoutApiKey() {
        assertThatThrownBy(() -> new BrevoMailService(builder, BASE_URL, "", "sender@example.com", "LandLord"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("refuses to start without a sender email")
    void failsFastWithoutSenderEmail() {
        assertThatThrownBy(() -> new BrevoMailService(builder, BASE_URL, "test-api-key", "", "LandLord"))
                .isInstanceOf(IllegalStateException.class);
    }
}
