package com.investview.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for UserCredentials entity.
 */
@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
}