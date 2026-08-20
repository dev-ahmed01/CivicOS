package com.civicos.notification.api;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;
import com.civicos.notification.application.NotificationResult;
import com.civicos.notification.application.NotificationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

	private final NotificationService notificationService;
	private final PageRequestFactory pageRequestFactory;

	public NotificationController(
			NotificationService notificationService,
			PageRequestFactory pageRequestFactory) {
		this.notificationService = notificationService;
		this.pageRequestFactory = pageRequestFactory;
	}

	@GetMapping
	public PagedResponse<NotificationResult> list(
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "createdAt,desc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(notificationService.listForCurrentUser(unreadOnly,
				pageRequestFactory.create(page, size, sort, Set.of("createdAt", "readAt", "type"))),
				CorrelationIdFilter.requestId(request));
	}

	@GetMapping("/unread-count")
	public Map<String, Long> unreadCount() {
		return Map.of("unreadCount", notificationService.unreadCount());
	}

	@PostMapping("/{notificationId}/read")
	public NotificationResult markRead(@PathVariable UUID notificationId) {
		return notificationService.markRead(notificationId);
	}

	@PostMapping("/read-all")
	public Map<String, Integer> markAllRead() {
		return Map.of("updated", notificationService.markAllRead());
	}
}
