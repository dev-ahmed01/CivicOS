package com.civicos.agency.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.agency.domain.Agency;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {
	Optional<Agency> findByCodeIgnoreCase(String code);
	List<Agency> findByActiveTrueOrderByNameAsc();
}
