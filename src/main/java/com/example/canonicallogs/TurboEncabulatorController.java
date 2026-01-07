package com.example.canonicallogs;

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

  public record RunResponse(double value) {}
}
