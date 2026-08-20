package com.civicos.common.web;

import java.util.List;

import org.springframework.data.domain.Page;

public record PagedResponse<T>(
		List<T> data,
		Pagination pagination,
		Meta meta) {

	public static <T> PagedResponse<T> from(Page<T> page, String requestId) {
		return new PagedResponse<>(
				page.getContent(),
				new Pagination(
						page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()),
				new Meta(requestId));
	}

	public record Pagination(int page, int size, long totalElements, int totalPages) {
	}

	public record Meta(String requestId) {
	}
}
