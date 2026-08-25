package com.idb.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import com.idb.auth.AuthApplication;
import com.idb.auth.common.constant.CommonConstants;
import com.idb.auth.common.constant.OperationStatus;
import com.idb.auth.common.dto.request.ApiPageRequest;
import com.idb.auth.common.dto.response.ApiPageResponse;
import com.idb.auth.constant.AuthConstants;
import com.idb.auth.dto.request.BearerAuthenticationToken;
import com.idb.auth.dto.response.PermissionResponse;
import com.idb.auth.model.Permission;

class ConstantsAndDtoTest {

    @Test
    @DisplayName("the IP-block exemption covers both unblock endpoints and nothing else")
    void ipBlockExemption() {
        assertThat(AuthConstants.isIpBlockExempt("/api/v3/ip-block/unblock")).isTrue();
        assertThat(AuthConstants.isIpBlockExempt("/api/v3/ip-block/unblock-user")).isTrue();
        assertThat(AuthConstants.isIpBlockExempt("/api/v3/ip-block/list")).isFalse();
        assertThat(AuthConstants.isIpBlockExempt("/api/v3/auth/login")).isFalse();
        assertThat(AuthConstants.isIpBlockExempt(null)).isFalse();
        // Not a prefix match on an unrelated path that merely contains the word.
        assertThat(AuthConstants.isIpBlockExempt("/api/v3/user/unblock")).isFalse();
    }

    @Test
    @DisplayName("initPublicUrls is idempotent, so repeated test contexts cannot grow the list")
    void initPublicUrlsIsIdempotent() {
        AuthApplication.initPublicUrls();
        int afterFirst = CommonConstants.PUBLIC_URLS.size();
        AuthApplication.initPublicUrls();
        AuthApplication.initPublicUrls();

        assertThat(CommonConstants.PUBLIC_URLS).hasSize(afterFirst);
        assertThat(CommonConstants.PUBLIC_URLS).contains("/api/v3/auth/**");
    }

    @Test
    @DisplayName("every public URL is a valid PathPattern - Spring Security 7 rejects suffix globs")
    void publicUrlsArePathPatternSafe() {
        AuthApplication.initPublicUrls();
        // A pattern like /**.html parses under AntPathMatcher but throws under
        // PathPattern, which would fail the whole filter chain at startup.
        for (String pattern : CommonConstants.PUBLIC_URLS) {
            assertThat(pattern).doesNotContain("**.");
            org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
                    .pathPattern(pattern);
        }
    }

    @Test
    @DisplayName("pageable defaults to unsorted and clamps a negative page number")
    void pageableDefaults() {
        ApiPageRequest<String> request = new ApiPageRequest<>();
        assertThat(request.getPageable()).isEqualTo(PageRequest.of(0, 10));

        request.setPageNumber(-5);
        assertThat(request.getPageable().getPageNumber()).isZero();

        request.setPageNumber(2);
        request.setPageSize(25);
        request.setSortColumn("ipAddress");
        request.setSortOrder(Direction.DESC);
        assertThat(request.getPageable())
                .isEqualTo(PageRequest.of(2, 25, Direction.DESC, "ipAddress"));
    }

    @Test
    @DisplayName("fromPage maps content and carries the pagination metadata")
    void apiPageResponseFromPage() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);
        ApiPageResponse<String> response = ApiPageResponse.fromPage(page, String::toUpperCase);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getData()).containsExactly("A", "B");
        assertThat(response.getPageNumber()).isZero();
        assertThat(response.getPageSize()).isEqualTo(2);
        assertThat(response.getTotalElements()).isEqualTo(5);
        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("fromPage with no mapper passes content through, and a null page degrades to an empty result")
    void apiPageResponseEdgeCases() {
        var page = new PageImpl<>(List.of("a"), PageRequest.of(0, 1), 1);
        assertThat(ApiPageResponse.fromPage(page, null).getData()).containsExactly("a");

        ApiPageResponse<String> empty = ApiPageResponse.fromPage(OperationStatus.SUCCESS, "none", null, null);
        assertThat(empty.getData()).isEmpty();
        assertThat(empty.getTotalPages()).isEqualTo(-1);

        ApiPageResponse<String> nullStatus = ApiPageResponse.fromPage(null, "msg");
        assertThat(nullStatus.getStatus()).isEqualTo(OperationStatus.ERROR);
    }

    @Test
    @DisplayName("PermissionResponse copies the fields the UI routes on")
    void permissionResponseMapping() {
        Permission permission = new Permission();
        permission.setId(7L);
        permission.setName("LIST_ROLES");
        permission.setUrl("/api/v3/role/list");
        permission.setRoute("/dashboard/roles");

        PermissionResponse response = new PermissionResponse(permission);
        assertThat(response.getId()).isEqualTo("7");
        assertThat(response.getName()).isEqualTo("LIST_ROLES");
        assertThat(response.getUrl()).isEqualTo("/api/v3/role/list");
        assertThat(response.getRoute()).isEqualTo("/dashboard/roles");

        // An unsaved permission has no id; mapping must not NPE.
        assertThat(new PermissionResponse(new Permission()).getId()).isNull();
    }

    @Test
    @DisplayName("permissions compare by name, so a re-parsed file entry equals its persisted row")
    void permissionEqualityIsByName() {
        Permission a = new Permission();
        a.setName("SAME");
        a.setId(1L);
        Permission b = new Permission();
        b.setName("SAME");
        b.setId(2L);
        Permission c = new Permission();
        c.setName("OTHER");

        assertThat(a).isEqualTo(b).isEqualTo(a).isNotEqualTo(c).isNotEqualTo("SAME");
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(new Permission().hashCode()).isZero();
    }

    @Test
    @DisplayName("a bearer token with null credentials means the access token was NOT rotated")
    void bearerTokenCredentialsSignalRotation() {
        BearerAuthenticationToken notRotated = new BearerAuthenticationToken("alice", null, true);
        assertThat(notRotated.getCredentials()).isNull();
        assertThat(notRotated.getName()).isEqualTo("alice");
        assertThat(notRotated.getPrincipal()).isEqualTo("alice");
        assertThat(notRotated.getDetails()).isEqualTo("alice");
        assertThat(notRotated.isAuthenticated()).isTrue();

        BearerAuthenticationToken rotated = new BearerAuthenticationToken("alice", "new.jwt.here", true);
        assertThat(rotated.getCredentials()).isEqualTo("new.jwt.here");

        rotated.setAuthenticated(false);
        assertThat(rotated.isAuthenticated()).isFalse();
        assertThat(rotated.getAuthorities()).isNull();
    }
}
