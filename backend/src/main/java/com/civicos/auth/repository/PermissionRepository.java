package com.civicos.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.auth.domain.Permission;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {
	Optional<Permission> findByCode(String code);
}
