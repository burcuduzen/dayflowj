package com.burcuduzen.dayflow.diagnostics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Path;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"dayflow.diagnostics.enabled=true","dayflow.desktop.managed=false","spring.datasource.url=jdbc:h2:mem:diagnostics-enabled"})
class DiagnosticsEnabledTests {
 @LocalServerPort int port;
 static final Path directory=temporaryDirectory();
 static Path temporaryDirectory(){try{return java.nio.file.Files.createTempDirectory("dayflow-diagnostics-test");}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}}
 @AfterAll static void cleanup()throws Exception{try(var paths=java.nio.file.Files.walk(directory)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(p);}}
 @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("dayflow.diagnostics.directory",()->directory.toString());}
 @Test void operationLogsExcludePayloadsAndMonitoringDoesNotCountItself()throws Exception {
  var http=HttpClient.newHttpClient();String root="http://127.0.0.1:"+port;
  var task=http.send(HttpRequest.newBuilder(URI.create(root+"/api/tasks")).header("Content-Type","application/json").header("X-DayFlowJ-Request","1").POST(HttpRequest.BodyPublishers.ofString("{\"title\":\"SENSITIVE-PAYLOAD\",\"priority\":\"MEDIUM\",\"estimatedMinutes\":30,\"reminderEnabled\":false}")).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(201,task.statusCode());
  var logs=http.send(HttpRequest.newBuilder(URI.create(root+"/api/diagnostics/logs")).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,logs.statusCode());assertFalse(logs.body().contains("SENSITIVE-PAYLOAD"));assertTrue(logs.body().contains("Görev"));
  var request=HttpRequest.newBuilder(URI.create(root+"/api/diagnostics/metrics")).GET().build();
  var first=http.send(request,HttpResponse.BodyHandlers.ofString());var second=http.send(request,HttpResponse.BodyHandlers.ofString());
  // History can change, but endpoint polls do not append operation logs.
  var after=http.send(HttpRequest.newBuilder(URI.create(root+"/api/diagnostics/logs")).GET().build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(logs.body(),after.body());assertEquals(200,first.statusCode());assertEquals(200,second.statusCode());
 }
}
