package com.barivara.backend.profile;

import com.idb.auth.common.exception.LogOnlyException;
import com.idb.auth.dto.request.UserRegistrationRequest;
import com.idb.auth.model.User;
import com.idb.auth.service.UserService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProfileController {

    private final TenantProfileRepository tenantRepository;
    private final OwnerProfileRepository ownerRepository;
    private final UserService userService;

    public ProfileController(TenantProfileRepository tenantRepository, OwnerProfileRepository ownerRepository,
            UserService userService) {
        this.tenantRepository = tenantRepository;
        this.ownerRepository = ownerRepository;
        this.userService = userService;
    }

    @GetMapping("/api/tenant-profiles/me")
    public TenantProfile myTenantProfile(@AuthenticationPrincipal User principal) {
        return tenantRepository.findFirstByAuthUserId(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tenant profile linked to this account"));
    }

    @GetMapping("/api/tenant-profiles/{id}")
    public TenantProfile tenant(@PathVariable Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record SignupRequest(String name, String email, String phone, String password) {}

    @PostMapping("/api/tenant-profiles")
    public ResponseEntity<TenantProfile> registerTenant(@RequestBody SignupRequest request) {
        User createdUser = createAuthUser(request, "TENANT");

        TenantProfile profile = new TenantProfile();
        profile.setName(request.name());
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        profile.setAuthUserId(createdUser.getId());

        TenantProfile saved = tenantRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/api/owner-profiles/me")
    public OwnerProfile myOwnerProfile(@AuthenticationPrincipal User principal) {
        return ownerRepository.findFirstByAuthUserId(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No owner profile linked to this account"));
    }

    @GetMapping("/api/owner-profiles/{id}")
    public OwnerProfile owner(@PathVariable Long id) {
        return ownerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/api/owner-profiles")
    public ResponseEntity<OwnerProfile> registerOwner(@RequestBody SignupRequest request) {
        User createdUser = createAuthUser(request, "OWNER");

        OwnerProfile profile = new OwnerProfile();
        profile.setName(request.name());
        profile.setEmail(request.email());
        profile.setPhone(request.phone());
        profile.setAuthUserId(createdUser.getId());

        OwnerProfile saved = ownerRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private User createAuthUser(SignupRequest request, String role) {
        UserRegistrationRequest userRequest = new UserRegistrationRequest();
        userRequest.setUsername(request.phone().replaceAll("[^a-zA-Z0-9]", ""));
        userRequest.setPassword(request.password());
        userRequest.setRoles(List.of(role));
        userRequest.setEmail(request.email());
        userRequest.setPhone(request.phone());
        try {
            return userService.registerUser(userRequest);
        } catch (LogOnlyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getResponse().getMessage());
        }
    }
}
