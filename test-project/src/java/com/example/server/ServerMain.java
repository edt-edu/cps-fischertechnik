package com.example.server;

import com.example.adapters.TestMachineAdapter;
import com.example.generated.testmachinemissions.TestMissionAlpha;
import com.example.generated.testmachinemissions.TestMissionBeta;
import com.example.runtime.rtc.time.ScheduledRuntimeScheduler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny HTTP server to interact with TestMachine adapters and missions.
 */
public final class ServerMain {
  public static void main(String[] args) throws Exception {
    ScheduledRuntimeScheduler scheduler = new ScheduledRuntimeScheduler();

    TestMachineAdapter machineA = new TestMachineAdapter("machineA", scheduler);
    TestMachineAdapter machineB = new TestMachineAdapter("machineB", scheduler);

    TestMissionAlpha alpha = new TestMissionAlpha(machineA, machineB);
    TestMissionBeta beta = new TestMissionBeta(machineB, machineA);

    alpha.start();
    beta.start();

    Map<String, TestMachineAdapter> machines = new HashMap<>();
    machines.put(machineA.id(), machineA);
    machines.put(machineB.id(), machineB);

    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
    server.createContext("/", new ApiHandler(machines, alpha, beta, server, scheduler));
    server.start();
    System.out.println("Server listening on http://localhost:8080/");
  }
  private static class ApiHandler implements HttpHandler {
    private final Map<String, TestMachineAdapter> machines;
    private final TestMissionAlpha alpha;
    private final TestMissionBeta beta;
    private final HttpServer server;
    private final ScheduledRuntimeScheduler scheduler;
    private final byte[] htmlBytes;

    ApiHandler(Map<String, TestMachineAdapter> machines, TestMissionAlpha alpha, TestMissionBeta beta, HttpServer server, ScheduledRuntimeScheduler scheduler) {
      this.machines = machines;
      this.alpha = alpha;
      this.beta = beta;
      this.server = server;
      this.scheduler = scheduler;
      this.htmlBytes = loadHtml();
    }

    private static byte[] loadHtml() {
      try (InputStream is = ApiHandler.class.getClassLoader().getResourceAsStream("index.html")) {
        if (is == null) return new byte[0];
        return is.readAllBytes();
      } catch (IOException e) {
        return new byte[0];
      }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String path = exchange.getRequestURI().getPath();
      String method = exchange.getRequestMethod();

      // Serve UI
      if ("GET".equals(method) && (path.equals("/") || path.equals("/index.html"))) {
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, htmlBytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(htmlBytes); }
        return;
      }

      // api status
      if ("GET".equals(method) && path.equals("/api/status")) {
        handleStatus(exchange);
        return;
      }

      // machines state update
      if ("POST".equals(method) && path.startsWith("/machines/") && path.endsWith("/state")) {
        String id = path.substring("/machines/".length(), path.length() - "/state".length());
        TestMachineAdapter m = machines.get(id);
        if (m == null) { send(exchange, 404, "not found"); return; }
        String body = readBody(exchange.getRequestBody());
        try {
          if (body.contains("attA")) {
            int idx = body.indexOf("attA") + 5; while (idx < body.length() && !Character.isDigit(body.charAt(idx)) && body.charAt(idx) != '-') idx++;
            int start = idx; while (idx < body.length() && (Character.isDigit(body.charAt(idx)) || body.charAt(idx) == '-')) idx++;
            if (start < idx) m.setAttA(Integer.parseInt(body.substring(start, idx)));
          }
          if (body.contains("attB")) {
            int idx = body.indexOf("attB") + 5; while (idx < body.length() && !Character.isDigit(body.charAt(idx)) && body.charAt(idx) != '-') idx++;
            int start = idx; while (idx < body.length() && (Character.isDigit(body.charAt(idx)) || body.charAt(idx) == '-')) idx++;
            if (start < idx) m.setAttB(Integer.parseInt(body.substring(start, idx)));
          }
          sendNoContent(exchange);
        } catch (Exception ex) { send(exchange, 400, "invalid state"); }
        return;
      }

      // validate / acknowledge command success
      if ("POST".equals(method) && path.startsWith("/machines/") && path.endsWith("/validate")) {
        String id = path.substring("/machines/".length(), path.length() - "/validate".length());
        TestMachineAdapter m = machines.get(id);
        if (m == null) { send(exchange, 404, "not found"); return; }
        m.acknowledgeCommandSuccess();
        sendNoContent(exchange);
        return;
      }

      // shutdown
      if ("POST".equals(method) && path.equals("/shutdown")) {
        new Thread(() -> {
          try { Thread.sleep(100); } catch (InterruptedException ignored) {}
          machines.values().forEach(TestMachineAdapter::shutdown);
          scheduler.shutdown();
          server.stop(0);
        }).start();
        send(exchange, 200, "shutting down");
        return;
      }

      send(exchange, 404, "unknown");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
      StringBuilder json = new StringBuilder();
      json.append("{\"machines\":[\n");
      boolean first = true;
      for (TestMachineAdapter m : machines.values()) {
        if (!first) json.append(",\n"); first = false;
          String missionState = m.id().equals("machineA") ? alpha.getActiveStateName() : beta.getActiveStateName();
          String cmdJson = m.getCurrentCommand() == null ? "null" : ("\"" + m.getCurrentCommand().name() + "\"");
          json.append(String.format("  {\"id\":\"%s\",\"command\":%s,\"attA\":%d,\"attB\":%d,\"missionState\":\"%s\"}", m.id(), cmdJson, m.getAttA(), m.getAttB(), missionState));
      }
      json.append("\n]}"); send(exchange, 200, json.toString());
    }

    private static String readBody(InputStream in) throws IOException { byte[] bytes = in.readAllBytes(); return new String(bytes, StandardCharsets.UTF_8); }

    private static void sendNoContent(HttpExchange ex) throws IOException { ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8"); ex.sendResponseHeaders(204, -1); try (OutputStream os = ex.getResponseBody()) {} }

    private static void send(HttpExchange ex, int code, String body) throws IOException { byte[] bytes = body.getBytes(StandardCharsets.UTF_8); ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8"); ex.sendResponseHeaders(code, bytes.length); try (OutputStream os = ex.getResponseBody()) { os.write(bytes); } }
  }
}
