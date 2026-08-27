package com.barivara.backend;

import static com.idb.auth.common.constant.CommonConstants.PUBLIC_GET_URLS;
import static com.idb.auth.common.constant.CommonConstants.PUBLIC_URLS;

import com.idb.auth.AuthApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Embeds com.idb.auth (Parts/auth) as a library: its own AuthApplication.main()
 * never runs, so its component/entity/repository packages need explicit scanning
 * here since they sit outside this app's own package root.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.barivara.backend", "com.idb.auth"})
@EntityScan(basePackages = {"com.barivara.backend", "com.idb.auth.model"})
@EnableJpaRepositories(basePackages = {"com.barivara.backend", "com.idb.auth.dao"})
public class BarivaraBackendApplication {

	public static void main(String[] args) {
		// Required: without this, AuthFilter treats /api/v3/auth/login itself as
		// protected because PUBLIC_URLS is only ever populated by this call, which
		// normally happens inside com.idb.auth's own main() - which we never invoke.
		AuthApplication.initPublicUrls();

		// Machine-to-machine sync calls from landlord-backend's BariVaraSyncService
		// (Phase 15) - no human token exists to attach to them, same trust level
		// (open, localhost-only) they had before this auth phase.
		PUBLIC_URLS.add("/api/listings/sync/vacancy-ad");
		PUBLIC_URLS.add("/api/listings/sync/unit-status");

		// Guest browsing: public marketplace search/listing-detail stay readable
		// with no login. Writes on these same paths (create/update/delete a
		// listing) still require the OWNER role via permissions.json.
		PUBLIC_GET_URLS.add("/api/listings");
		PUBLIC_GET_URLS.add("/api/listings/*");

		// Self-service signup can't require the very role it's trying to grant -
		// these exact bare paths carry only the POST-to-register mapping (GET
		// lives at /me and /{id}, both still role-gated via permissions.json).
		PUBLIC_URLS.add("/api/tenant-profiles");
		PUBLIC_URLS.add("/api/owner-profiles");

		SpringApplication.run(BarivaraBackendApplication.class, args);
	}

}
