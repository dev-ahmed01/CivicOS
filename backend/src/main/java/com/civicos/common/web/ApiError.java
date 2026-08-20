package com.civicos.common.web;

public record ApiError(String code, String message, String requestId) {
}
