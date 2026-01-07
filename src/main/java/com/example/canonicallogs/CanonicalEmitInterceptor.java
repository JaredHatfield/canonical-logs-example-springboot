package com.example.canonicallogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.Map;

@Component
public class CanonicalEmitInterceptor implements HandlerInterceptor {

  private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

  private final ObjectProvider<CanonicalLogContext> ctxProvider;
  private final ObjectMapper mapper;
  private final CanonicalLogger canonicalLogger;

  public CanonicalEmitInterceptor(ObjectProvider<CanonicalLogContext> ctxProvider,
                                  ObjectMapper mapper,
                                  CanonicalLogger canonicalLogger) {
    this.ctxProvider = ctxProvider;
    this.mapper = mapper;
    this.canonicalLogger = canonicalLogger;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    CanonicalLogContext ctx = ctxProvider.getObject();

    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    if (pattern != null) {
      ctx.put("http.route", pattern.toString());
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler,
                              Exception ex) {

    CanonicalLogContext ctx = ctxProvider.getObject();

    // Ensures exactly one canonical record emitted
    if (!ctx.markEmittedIfFirst()) return;

    Instant end = Instant.now();
    long durationMs = end.toEpochMilli() - ctx.start().toEpochMilli();

    ctx.put("ts", end.toString());
    ctx.put("duration_ms", durationMs);
    ctx.put("http.status_code", response.getStatus());

    if (ex != null) {
      ctx.put("outcome", "failure");
      ctx.put("error_type", ex.getClass().getSimpleName());
      ctx.put("error_message", safeMessage(ex.getMessage()));
    } else {
      ctx.put("outcome", outcomeFromStatus(response.getStatus()));
    }

    try {
      Map<String, Object> payload = ctx.snapshot();
      canonicalLogger.info(mapper.writeValueAsString(payload));
    } catch (Exception emitError) {
      // Never break request completion because logging failed
      canonicalLogger.info("{\"kind\":\"http\",\"outcome\":\"failure\",\"error_type\":\"CanonicalEmitFailed\"}");
    }
  }

  private static String outcomeFromStatus(int status) {
    if (status >= 200 && status < 400) return "success";
    if (status == 408) return "timeout";
    if (status == 429) return "rejected";
    return "failure";
  }

  private static String safeMessage(String msg) {
    if (msg == null) return null;
    String t = msg.trim();
    return t.length() > MAX_ERROR_MESSAGE_LENGTH ? t.substring(0, MAX_ERROR_MESSAGE_LENGTH) : t;
  }
}
