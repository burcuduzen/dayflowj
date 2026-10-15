package com.burcuduzen.dayflow.diagnostics;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"dayflow.diagnostics.enabled=false","dayflow.desktop.managed=false","spring.datasource.url=jdbc:h2:mem:diagnostics-disabled"})
class DiagnosticsDisabledTests {
 @LocalServerPort int port;
 @Autowired ApplicationContext context;
 @Test void normalUserCanAccessPageAndMetricsWithoutDeveloperFlag()throws Exception {
  assertFalse(context.getBeansOfType(DiagnosticsService.class).isEmpty());
  var http=HttpClient.newHttpClient();
  for(String path:new String[]{"/diagnostics.html","/api/diagnostics/metrics","/api/diagnostics/logs"}) {
   var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());
   assertEquals(200,response.statusCode(),path);
  }
 }
}
