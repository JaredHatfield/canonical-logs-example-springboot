package com.example.canonicallogs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class CanonicalInitFilter extends OncePerRequestFilter {

  private final ObjectProvider<CanonicalLogContext> ctxProvider;
  private final Environment env;
  private final AppRuntimeProperties runtime;

  public CanonicalInitFilter(ObjectProvider<CanonicalLogContext> ctxProvider,
                             Environment env,
                             AppRuntimeProperties runtime) {
    this.ctxProvider = ctxProvider;
    this.env = env;
    this.runtime = runtime;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req,
                                  HttpServletResponse res,
                                  FilterChain chain) throws IOException, jakarta.servlet.ServletException {

    CanonicalLogContext ctx = ctxProvider.getObject();

    String requestId = firstNonBlank(req.getHeader("X-Request-Id"), "req_" + UUID.randomUUID());

    ctx.put("ts_start", ctx.start().toString());
    ctx.put("service", env.getProperty("spring.application.name", "unknown-service"));
    ctx.put("env", runtime.env());
    ctx.put("region", runtime.region());
    ctx.put("version", runtime.version());

    ctx.put("kind", "http");
    ctx.put("request_id", requestId);

    ctx.put("http.method", req.getMethod());
    ctx.put("http.target", req.getRequestURI());

    chain.doFilter(req, res);
  }

  private static String firstNonBlank(String a, String b) {
    return Optional.ofNullable(a).filter(s -> !s.isBlank()).orElse(b);
  }
}
