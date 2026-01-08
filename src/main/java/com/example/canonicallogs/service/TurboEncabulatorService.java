package com.example.canonicallogs.service;

import com.example.canonicallogs.logging.CanonicalLogContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class TurboEncabulatorService {

  private final ObjectProvider<CanonicalLogContext> logCtxProvider;
  private final ThreadPoolTaskExecutor churnExecutor;

  public TurboEncabulatorService(ObjectProvider<CanonicalLogContext> logCtxProvider,
                                 @Qualifier("churnExecutor") ThreadPoolTaskExecutor churnExecutor) {
    this.logCtxProvider = logCtxProvider;
    this.churnExecutor = churnExecutor;
  }

  public double computeRunValue(String turboId) {
    CanonicalLogContext logCtx = logCtxProvider.getObject();
    long startNs = System.nanoTime();

    // Example work: random number generator
    double value = ThreadLocalRandom.current().nextDouble(0.0, 10.0);

    long durationMs = (System.nanoTime() - startNs) / 1_000_000;

    logCtx.put("compute.strategy", "random");
    logCtx.put("compute.duration_ms", durationMs);
    logCtx.put("turboencabulator.value", value);

    return value;
  }

  public void performChurn(String turboId, CanonicalLogContext context) {
    // Generate random churn time (1-1000ms)
    int churnTimeMs = ThreadLocalRandom.current().nextInt(1, 1001);
    
    // Capture the current request attributes to propagate to background thread
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      context.put("churn_error", "no_request_context");
      return;
    }
    
    // Submit task to executor and wait for completion
    Future<?> future = churnExecutor.submit(() -> {
      try {
        // Set request attributes in background thread so scoped beans work
        RequestContextHolder.setRequestAttributes(requestAttributes);
        
        Thread.sleep(churnTimeMs);
        // Now we can use the context parameter which should resolve correctly
        context.put("churn_time", churnTimeMs);
        context.put("churn_thread", Thread.currentThread().getName());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        context.put("churn_interrupted", true);
      } catch (Exception e) {
        // Catch any other exception to see what's going wrong
        context.put("churn_task_error", e.getMessage());
      } finally {
        // Clean up request attributes
        RequestContextHolder.resetRequestAttributes();
      }
    });
    
    // Wait for the task to complete so churn_time is included in the canonical log
    try {
      future.get(2, TimeUnit.SECONDS); // Wait up to 2 seconds for the task
    } catch (TimeoutException e) {
      context.put("churn_error", "timeout");
    } catch (ExecutionException e) {
      context.put("churn_error", "execution_failed");
      context.put("churn_error_message", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    } catch (Exception e) {
      context.put("churn_error", "failed_to_complete");
      context.put("churn_error_type", e.getClass().getSimpleName());
    }
  }
}
