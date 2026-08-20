package com.civicos.common.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";
	public static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".requestId";
	private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String supplied = request.getHeader(HEADER);
		String requestId = supplied != null && SAFE_ID.matcher(supplied).matches()
				? supplied
				: UUID.randomUUID().toString();
		request.setAttribute(ATTRIBUTE, requestId);
		response.setHeader(HEADER, requestId);
		try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
			filterChain.doFilter(request, response);
		}
	}

	public static String requestId(HttpServletRequest request) {
		Object value = request.getAttribute(ATTRIBUTE);
		return value == null ? "unknown" : value.toString();
	}
}
