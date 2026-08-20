package com.civicos.road.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.civicos.road.domain.Road;

public interface RoadRepository extends JpaRepository<Road, UUID> {
	Optional<Road> findByExternalReference(String externalReference);
	List<Road> findByActiveTrueOrderByNameAsc();
}
