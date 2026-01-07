package com.example.canonicallogs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CanonicalLogger {
  private final Logger log = LoggerFactory.getLogger("canonical");
  public void info(String json) { log.info(json); }
}
