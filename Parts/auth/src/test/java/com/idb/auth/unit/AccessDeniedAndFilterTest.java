package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.idb.auth.AuthApplication;
import com.idb.auth.config.AuthAccessDeniedHandler;
import com.idb.auth.config.AuthEntryPoint;
import com.idb.auth.config.AuthManager;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.filter.AuthFilter;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessDeniedAndFilterTest {

    @Mock private AuthManager authManager;

    private AuthEntryPoint entryPoint;
    private AuthFilter authFilter;
    private AuthAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {
        AuthApplication.initPublicUrls();
        entryPoint = new AuthEntryPoint(JsonMapper.builder().build());
        accessDeniedHandler = new AuthAccessDeniedHandler(JsonMapper.builder().build());
        authFilter = new AuthFilter(authManager, entryPoint);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("the access-denied handler commits 403 itself instead of calling sendError")
    void accessDeniedCommitsItsOwnResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v3/test/admin-only");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("Access Denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":\"ACCESS_DENIED\"");
        // sendError would trigger a container ERROR dispatch back through the
        // security chain, where AuthFilter (a OncePerRequestFilter) is skipped - the
        // empty context there turns an honest 403 into a misleading 401.
        assertThat(response.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("public URLs bypass authentication and continue the chain")
    void publicUrlsAreNotFiltered() throws Exception {
        // Asserted through behaviour rather than by calling the protected
        // shouldNotFilter: what matters is that no authentication is attempted and
        // the request still reaches the controller.
        for (String path : new String[] { "/api/v3/auth/login", "/api/v3/auth/otp",
                "/api/v3/auth/forgot-password", "/actuator/health" }) {
            MockFilterChain chain = new MockFilterChain();
            authFilter.doFilter(new MockHttpServletRequest("POST", path),
                    new MockHttpServletResponse(), chain);
            assertThat(chain.getRequest()).as(path).isNotNull();
        }
        verify(authManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("protected URLs are authenticated before the chain continues")
    void protectedUrlsAreFiltered() throws Exception {
        when(authManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("alice", null));

        for (String path : new String[] { "/api/v3/test/any", "/api/v3/user/logout-all",
                "/api/v3/role/list", "/api/v3/permission/get-user-permissions" }) {
            authFilter.doFilter(new MockHttpServletRequest("GET", path),
                    new MockHttpServletResponse(), new MockFilterChain());
        }
        verify(authManager, org.mockito.Mockito.times(4)).authenticate(any());
    }

    @Test
    @DisplayName("a successful authentication is placed in the context and the chain continues")
    void successPopulatesTheContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v3/test/any");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        Authentication auth = new UsernamePasswordAuthenticationToken("alice", null);
        when(authManager.authenticate(any())).thenReturn(auth);

        authFilter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(auth);
        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("a rotated access token is echoed in the x-access-token response header")
    void rotatedTokenIsEchoed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v3/test/any");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authManager.authenticate(any()))
                .thenReturn(new BearerAuthenticationToken("alice", "brand.new.token", true));

        authFilter.doFilter(request, response, new MockFilterChain());

        // This header is the only way a client learns its access token was renewed
        // off the refresh token.
        assertThat(response.getHeader("x-access-token")).isEqualTo("brand.new.token");
    }

    @Test
    @DisplayName("a non-rotated authentication emits no token header")
    void noRotationMeansNoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v3/test/any");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authManager.authenticate(any()))
                .thenReturn(new BearerAuthenticationToken("alice", null, true));

        authFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader("x-access-token")).isNull();
    }

    @Test
    @DisplayName("an authentication failure is rendered by the entry point and the chain is NOT continued")
    void failureDelegatesToEntryPointAndStops() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v3/test/any");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));

        authFilter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"status\":\"BAD_CREDENTIALS\"");
        // Letting the exception propagate instead would escape the security chain
        // and get flattened to HTTP 200 by GlobalExceptionFilter.
        assertThat(chain.getRequest()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("a failed authentication leaves no residue in the security context")
    void failureClearsTheContext() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stale", null));
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));

        authFilter.doFilter(new MockHttpServletRequest("GET", "/api/v3/test/any"),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("a public request never reaches the authentication manager")
    void publicRequestSkipsAuthentication() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        authFilter.doFilter(new MockHttpServletRequest("POST", "/api/v3/auth/login"),
                new MockHttpServletResponse(), chain);

        verify(authManager, never()).authenticate(any());
        assertThat(chain.getRequest()).isNotNull();
    }
}
