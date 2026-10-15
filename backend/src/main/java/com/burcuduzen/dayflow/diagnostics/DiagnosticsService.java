package com.burcuduzen.dayflow.diagnostics;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import java.lang.management.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
public class DiagnosticsService {
    private final DiagnosticStore store;
    private final Deque<Map<String,Object>> samples=new ArrayDeque<>();
    private final com.sun.management.OperatingSystemMXBean os;
    private final int cores;
    private long previousCpu=-1,previousWall=-1;
    public DiagnosticsService(@Value("${dayflow.diagnostics.directory:}")String directory) {
        String data=System.getenv("DAYFLOW_DATA_DIR");
        Path base=directory.isBlank()?Path.of(data==null||data.isBlank()?"data":data).resolve("diagnostics"):Path.of(directory);
        store=new DiagnosticStore(base);
        var bean=ManagementFactory.getOperatingSystemMXBean();
        os=bean instanceof com.sun.management.OperatingSystemMXBean extended?extended:null;
        cores=Math.max(1,bean.getAvailableProcessors());
    }
    @EventListener(ApplicationReadyEvent.class)public void ready(){store.record("INFO","Uygulama başlatıldı",0,0,false);}
    @EventListener(ContextClosedEvent.class)public void close(){store.record("INFO","Uygulama kapatılıyor",0,0,false);}
    @Scheduled(fixedDelay=5000)public synchronized void sample() {
        long wall=System.nanoTime(),cpu=os==null?-1:os.getProcessCpuTime();
        Double percent=null;
        if(cpu>=0&&previousCpu>=0&&wall>previousWall)percent=Math.max(0,Math.min(100,100.0*(cpu-previousCpu)/(wall-previousWall)/cores));
        previousCpu=cpu;previousWall=wall;
        var memory=ManagementFactory.getMemoryMXBean();var heap=memory.getHeapMemoryUsage();
        Map<String,Object> s=new LinkedHashMap<>();s.put("time",Instant.now());s.put("pid",ProcessHandle.current().pid());
        s.put("cpuPercent",percent);s.put("cores",cores);s.put("heapUsedBytes",heap.getUsed());s.put("heapMaxBytes",heap.getMax());
        s.put("nonHeapUsedBytes",memory.getNonHeapMemoryUsage().getUsed());s.put("uptimeMs",ManagementFactory.getRuntimeMXBean().getUptime());
        s.put("threads",ManagementFactory.getThreadMXBean().getThreadCount());samples.addLast(s);if(samples.size()>60)samples.removeFirst();
    }
    public synchronized Map<String,Object> snapshot(){if(samples.isEmpty())sample();return Map.of("latest",samples.getLast(),"history",List.copyOf(samples),"requests",store.requests());}
    public List<DiagnosticStore.Entry> entries(String level,String search,int limit){return store.entries(level,search,limit);}
    public void event(String level,String action){store.record(level,action,0,0,false);}
    public void request(String method,String path,int status,double millis) {
        if(!path.startsWith("/api/")||path.startsWith("/api/diagnostics"))return;
        // Resource identifiers and unknown URLs never enter the logs.
        String resource=path.split("/").length>2?path.split("/")[2]:"";
        String name=switch(resource){case "tasks"->"Görev";case "notes"->"Not";case "calendar-events"->"Takvim etkinliği";case "gmail"->"Gmail işlemi";
            case "email"->"Mail bildirimi";case "reminders"->"Hatırlatma";case "planning"->"Planlama";case "settings"->"Ayarlar";case "backups"->"Yedekleme";default->"API işlemi";};
        String action=switch(method){case "GET"->"okuma";case "POST"->"oluşturma / çalıştırma";case "PUT","PATCH"->"güncelleme";case "DELETE"->"silme";default->"istek";};
        // Latency includes reads; log history records mutations and failed reads.
        if(!method.equals("GET")||status>=400)store.record(status>=500?"ERROR":status>=400?"WARN":"INFO",name+" · "+action,status,millis,true);
        else recordRead(millis);
    }
    private void recordRead(double millis){store.observe(200,millis);}
}
