package com.landlord.backend;

import static com.idb.auth.common.constant.CommonConstants.PUBLIC_URLS;

import com.idb.auth.AuthApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Embeds com.idb.auth (Parts/auth) as a library: its own AuthApplication.main()
 * never runs, so its component/entity/repository packages need explicit scanning
 * here since they sit outside this app's own package root.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.landlord.backend", "com.idb.auth"})
@EntityScan(basePackages = {"com.landlord.backend", "com.idb.auth.model"})
@EnableJpaRepositories(basePackages = {"com.landlord.backend", "com.idb.auth.dao"})
public class LandlordBackendApplication {

	public static void main(String[] args) {
		// Required: without this, AuthFilter treats /api/v3/auth/login itself as
		// protected because PUBLIC_URLS is only ever populated by this call, which
		// normally happens inside com.idb.auth's own main() - which we never invoke.
		AuthApplication.initPublicUrls();

		// Machine-to-machine sync call from barivara-backend's LandlordSyncService
		// (Phase 15) - no human token exists to attach to it, same trust level
		// (open, localhost-only) it had before this auth phase.
		PUBLIC_URLS.add("/api/marketplace-requests/from-barivara");

		SpringApplication.run(LandlordBackendApplication.class, args);
	}

}
