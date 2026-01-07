package com.example.canonicallogs;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequestScope
public class CanonicalLogContext {

  private final Instant start = Instant.now();

  // Request-scoped already isolates per request.
  // synchronizedMap makes it resilient if someone does parallel work inside one request.
  private final Map<String, Object> fields =
      Collections.synchronizedMap(new LinkedHashMap<>());

  private final AtomicBoolean emitted = new AtomicBoolean(false);

  public Instant start() { return start; }

  public void put(String key, Object value) {
    if (value != null) fields.put(key, value);
  }

  public Map<String, Object> snapshot() {
    synchronized (fields) {
      return new LinkedHashMap<>(fields);
    }
  }

  public boolean markEmittedIfFirst() {
    return emitted.compareAndSet(false, true);
  }
}
