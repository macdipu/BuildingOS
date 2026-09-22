package com.buildingos.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public final class CorrelationFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern UUID_TEXT = Pattern.compile("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationFilter.class);

    public static String traceId(HttpServletRequest request) {
        Object id = request.getAttribute(HEADER);
        return id == null ? UUID.randomUUID().toString() : id.toString();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER);
        String trace = supplied != null && UUID_TEXT.matcher(supplied).matches()
                ? supplied.toLowerCase(java.util.Locale.ROOT) : UUID.randomUUID().toString();
        request.setAttribute(HEADER, trace);
        response.setHeader(HEADER, trace);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", trace)) {
            chain.doFilter(request, response);
            LOG.info("http_request method={} status={}", request.getMethod(), response.getStatus());
        }
    }
}
