package com.example.canonicallogs.service;

import com.example.canonicallogs.logging.CanonicalLogContext;
import com.example.canonicallogs.logging.CanonicalLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TurboEncabulatorService {

  private final CanonicalLogContext logCtx;
  private final Executor churnExecutor;
  private final CanonicalLogger canonicalLogger;
  private final ObjectMapper objectMapper;

  public TurboEncabulatorService(CanonicalLogContext logCtx,
                                 @Qualifier("churnExecutor") Executor churnExecutor,
                                 CanonicalLogger canonicalLogger,
                                 ObjectMapper objectMapper) {
    this.logCtx = logCtx;
    this.churnExecutor = churnExecutor;
    this.canonicalLogger = canonicalLogger;
    this.objectMapper = objectMapper;
  }

  public double computeRunValue(String turboId) {
    long startNs = System.nanoTime();

    // Example work: random number generator
    double value = ThreadLocalRandom.current().nextDouble(0.0, 10.0);

    long durationMs = (System.nanoTime() - startNs) / 1_000_000;

    logCtx.put("compute.strategy", "random");
    logCtx.put("compute.duration_ms", durationMs);
    logCtx.put("turboencabulator.value", value);

    return value;
  }

  public void performChurn(String turboId, String requestId) {
    // Generate random churn time (1-1000ms)
    int churnTimeMs = ThreadLocalRandom.current().nextInt(1, 1001);
    
    // Dispatch task to executor
    churnExecutor.execute(() -> {
      try {
        Thread.sleep(churnTimeMs);
        // Log from background thread with structured logging using ObjectMapper
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("request_id", requestId);
        logData.put("turboencabulator.id", turboId);
        logData.put("churn_time", churnTimeMs);
        logData.put("thread", Thread.currentThread().getName());
        
        canonicalLogger.info(objectMapper.writeValueAsString(logData));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        // Log error but don't break the thread
        canonicalLogger.info("{\"error\":\"Failed to log churn completion\"}");
      }
    });
    
    // Log after task dispatch but before it completes
    logCtx.put("churn_dispatched", true);
  }
}
