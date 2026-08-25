package com.idb.auth.common.service;

import com.idb.auth.common.dto.MailInfo;
import com.idb.auth.common.exception.TraceableException;

/**
 * Outbound mail seam. The auth flow only needs OTP delivery; keeping this an
 * interface means integration tests can replace it with a mock and read the
 * generated OTP, which is otherwise only stored hashed.
 */
public interface MailService {
    void sendTemplatedEmail(MailInfo mailInfo) throws TraceableException;
}
