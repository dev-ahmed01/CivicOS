package com.civicos.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.civicos.user.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	@EntityGraph(attributePaths = {"agency", "roles", "roles.permissions"})
	Optional<User> findByEmailIgnoreCase(String email);

	@EntityGraph(attributePaths = {"agency", "roles", "roles.permissions"})
	@Query("select user from User user where user.id = :id")
	Optional<User> findWithAuthoritiesById(@Param("id") UUID id);

	List<User> findByAgencyIdAndStatus(UUID agencyId, User.Status status);
}
