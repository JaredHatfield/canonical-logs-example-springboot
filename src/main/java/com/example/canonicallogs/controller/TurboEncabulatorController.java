package com.example.canonicallogs.controller;

import com.example.canonicallogs.logging.CanonicalLogContext;
import com.example.canonicallogs.service.TurboEncabulatorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/turboencabulators")
public class TurboEncabulatorController {

  private final CanonicalLogContext logCtx;
  private final TurboEncabulatorService service;

  public TurboEncabulatorController(CanonicalLogContext logCtx, TurboEncabulatorService service) {
    this.logCtx = logCtx;
    this.service = service;
  }

  @PostMapping("/{id}/runs")
  public RunResponse run(@PathVariable("id") String turboId,
                         @RequestHeader(value = "X-User-Id", required = false) String userId) {

    logCtx.put("turboencabulator.id", turboId);
    if (userId != null && !userId.isBlank()) {
      logCtx.put("user_id", userId);
    }

    double value = service.computeRunValue(turboId);
    return new RunResponse(value);
  }

  @PostMapping("/{id}/churn")
  public ChurnResponse churn(@PathVariable("id") String turboId,
                             @RequestHeader(value = "X-User-Id", required = false) String userId) {

    logCtx.put("turboencabulator.id", turboId);
    if (userId != null && !userId.isBlank()) {
      logCtx.put("user_id", userId);
    }

    // Get request ID from context to pass to background task
    String requestId = (String) logCtx.snapshot().get("request_id");
    
    service.performChurn(turboId, requestId);
    
    return new ChurnResponse("dispatched");
  }

  public record RunResponse(double value) {}
  public record ChurnResponse(String status) {}
}
