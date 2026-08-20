package com.civicos.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.user.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByEmailIgnoreCase(String email);
	List<User> findByAgencyIdAndStatus(UUID agencyId, User.Status status);
}
