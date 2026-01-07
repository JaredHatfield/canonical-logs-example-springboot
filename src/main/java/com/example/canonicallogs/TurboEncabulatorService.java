package com.example.canonicallogs;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class TurboEncabulatorService {

  private final CanonicalLogContext logCtx;

  public TurboEncabulatorService(CanonicalLogContext logCtx) {
    this.logCtx = logCtx;
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
}
