package com.idb.auth.dao;

import static com.idb.auth.common.constant.CommonConstants.CACHE_USER;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.idb.auth.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Cached because every authenticated request loads the user to obtain the JWT
     * signing secret (the password hash). {@link #save} writes through, and the
     * revoke/soft-delete mutations evict, so the cache cannot serve a stale
     * password hash or a stale revocation watermark.
     */
    @Cacheable(value = CACHE_USER, key = "#username", condition = "#username != null")
    @Query(value = "SELECT * FROM users WHERE username = :username AND is_active = true", nativeQuery = true)
    Optional<User> findByUsername(String username);

    @Query(value = "SELECT * FROM users WHERE id = :id AND is_active = true", nativeQuery = true)
    Optional<User> findById(Long id);

    @CachePut(value = CACHE_USER, key = "#result?.username",
            condition = "#result != null && #result.id != null && #result.username != null")
    @Override
    <S extends User> S save(S user);

    @CacheEvict(value = CACHE_USER, key = "#username", condition = "#username != null")
    @Modifying
    @Query(value = "UPDATE users SET is_active = false WHERE username = :username OR email = :username",
            nativeQuery = true)
    int softDelete(String username);

    @CacheEvict(value = CACHE_USER, key = "#username", condition = "#username != null")
    @Modifying
    @Query(value = "UPDATE users SET tokens_valid_after = :timestamp WHERE username = :username", nativeQuery = true)
    int revokeTokensIssuedBefore(@Param("username") String username, @Param("timestamp") LocalDateTime timestamp);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM users u WHERE u.username = :username)", nativeQuery = true)
    boolean existsByUsername(String username);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM users u WHERE u.email = :email)", nativeQuery = true)
    boolean existsByEmail(String email);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM users u WHERE u.phone = :phone)", nativeQuery = true)
    boolean existsByPhone(String phone);

    @Query(value = "SELECT * FROM users WHERE account_locked = true AND is_active = true", nativeQuery = true)
    List<User> findAllLockedUsers();
}
