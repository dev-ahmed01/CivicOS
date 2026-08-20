package com.civicos.common.web;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.civicos.common.domain.DomainValidationException;

@Component
public class PageRequestFactory {

	public Pageable create(int page, int size, String sort, Set<String> allowedSorts) {
		if (page < 0) {
			throw new DomainValidationException("Page must be zero or greater.");
		}
		if (size < 1 || size > 100) {
			throw new DomainValidationException("Page size must be between 1 and 100.");
		}
		String[] parts = sort == null || sort.isBlank()
				? new String[] { "createdAt", "desc" }
				: sort.split(",", -1);
		if (parts.length > 2 || !allowedSorts.contains(parts[0])) {
			throw new DomainValidationException("Unsupported sort field.");
		}
		Sort.Direction direction;
		try {
			direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
		} catch (IllegalArgumentException exception) {
			throw new DomainValidationException("Sort direction must be asc or desc.");
		}
		return PageRequest.of(page, size, Sort.by(direction, parts[0]));
	}
}
