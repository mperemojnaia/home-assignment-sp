package com.bank.statementprocessor.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that sets up correlation ID in MDC for request tracking.
 * Runs early in the filter chain to ensure correlation ID is available for all logging.
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter implements Filter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Use correlation ID from header if present, otherwise generate new one
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Set in MDC for logging
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            // Clean up specific key, not MDC.clear() which could affect other threads
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
