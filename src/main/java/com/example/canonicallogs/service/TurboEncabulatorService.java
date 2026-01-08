package com.example.canonicallogs.service;

import com.example.canonicallogs.logging.CanonicalLogContext;
import com.example.canonicallogs.logging.CanonicalLogger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TurboEncabulatorService {

  private final CanonicalLogContext logCtx;
  private final Executor churnExecutor;
  private final CanonicalLogger canonicalLogger;

  public TurboEncabulatorService(CanonicalLogContext logCtx,
                                 @Qualifier("churnExecutor") Executor churnExecutor,
                                 CanonicalLogger canonicalLogger) {
    this.logCtx = logCtx;
    this.churnExecutor = churnExecutor;
    this.canonicalLogger = canonicalLogger;
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
        // Log from background thread with structured logging
        canonicalLogger.info(String.format(
            "{\"request_id\":\"%s\",\"turboencabulator.id\":\"%s\",\"churn_time\":%d,\"thread\":\"%s\"}",
            requestId, turboId, churnTimeMs, Thread.currentThread().getName()));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    
    // Log after task dispatch but before it completes
    logCtx.put("churn_dispatched", true);
  }
}
