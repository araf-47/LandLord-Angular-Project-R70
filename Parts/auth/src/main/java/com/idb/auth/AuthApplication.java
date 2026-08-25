package com.idb.auth;

import static com.idb.auth.common.constant.CommonConstants.PUBLIC_URLS;
import static com.idb.auth.constant.AuthConstants.AUTH_PUBLIC_URLS;

import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {

	public static void main(String[] args) {
		initPublicUrls();
		SpringApplication.run(AuthApplication.class, args);
	}

	/**
	 * Aggregates every module's public-URL list into
	 * {@link com.idb.auth.common.constant.CommonConstants#PUBLIC_URLS}.
	 *
	 * <p>Called from {@link #main(String[])} before the Spring context starts.
	 * Exposed because this runs outside the Spring lifecycle: a
	 * {@code @SpringBootTest} never invokes {@code main}, so without calling this
	 * the list stays empty and {@code AuthFilter} treats every public endpoint
	 * (including login) as protected. Integration tests call it directly so they
	 * exercise the same URL set as production.
	 *
	 * <p>Guards against re-adding entries on a second call - harmless for matching,
	 * but the list would grow without bound across repeated test contexts.
	 */
	public static void initPublicUrls() {
		List<String> allUrls = new LinkedList<>(AUTH_PUBLIC_URLS);
		for (String url : allUrls) {
			if (!PUBLIC_URLS.contains(url)) {
				PUBLIC_URLS.add(url);
			}
		}
	}
}
