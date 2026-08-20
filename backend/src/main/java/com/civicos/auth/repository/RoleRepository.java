package com.civicos.auth.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.auth.domain.Role;

public interface RoleRepository extends JpaRepository<Role, UUID> {
	Optional<Role> findByCode(String code);
	List<Role> findAllByOrderByCodeAsc();
	List<Role> findByCodeIn(Set<String> codes);
}
