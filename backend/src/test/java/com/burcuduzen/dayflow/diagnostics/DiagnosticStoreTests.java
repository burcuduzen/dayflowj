package com.burcuduzen.dayflow.diagnostics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class DiagnosticStoreTests {
 @TempDir Path directory;
 @Test void metricsIncludeReadsWithoutFillingActivityHistory(){var s=new DiagnosticStore(directory);s.observe(200,10);s.record("ERROR","Görev · oluşturma",500,100,true);assertEquals(2L,s.requests().get("count"));assertEquals(1L,s.requests().get("errors"));assertEquals(1,s.entries(null,null,300).size());assertEquals(100.0,s.requests().get("p95Ms"));}
 @Test void ringFiltersAndRestartHistoryAreBounded(){var s=new DiagnosticStore(directory);for(int i=0;i<350;i++)s.record("INFO","Görev · güncelleme",200,i,true);s.record("ERROR","Gmail kontrolü başarısız",0,0,false);assertEquals(300,s.entries(null,null,1000).size());assertEquals(1,s.entries("ERROR","Gmail",10).size());var loaded=new DiagnosticStore(directory);assertEquals(300,loaded.entries(null,null,300).size());assertEquals(0L,loaded.requests().get("count"));}
 @Test void diskFailureDoesNotBreakOperations(){var s=new DiagnosticStore(directory.resolve("unavailable"));try{Files.delete(directory.resolve("unavailable"));Files.writeString(directory.resolve("unavailable"),"file");}catch(Exception ex){throw new RuntimeException(ex);}assertDoesNotThrow(()->s.record("INFO","Görev",200,5,true));assertEquals(false,s.requests().get("storageAvailable"));}
}
