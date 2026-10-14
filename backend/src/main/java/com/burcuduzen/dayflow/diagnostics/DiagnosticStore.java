package com.burcuduzen.dayflow.diagnostics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Bounded operation history; no bodies, query strings or exception messages. */
public class DiagnosticStore {
    public record Entry(long id, Instant time, String level, String action, int status, double durationMs) {}
    private final Deque<Entry> entries = new ArrayDeque<>();
    private final Deque<Double> durations = new ArrayDeque<>();
    private final Path file;
    private long sequence, requests, errors;
    private boolean storageAvailable=true;
    public DiagnosticStore(Path directory) {
        file=directory.resolve("activity.tsv");
        try {
            Files.createDirectories(directory);
            // Read at most the bounded current file. IDs remain stable across restarts.
            if(Files.exists(file)&&Files.size(file)<=524288)for(String line:Files.readAllLines(file,StandardCharsets.UTF_8)) {
                try {String[] p=line.split("\\t");Entry e=new Entry(Long.parseLong(p[0]),Instant.parse(p[1]),p[2],p[3],Integer.parseInt(p[4]),Double.parseDouble(p[5]));
                    entries.addLast(e);sequence=Math.max(sequence,e.id());if(entries.size()>300)entries.removeFirst();
                }catch(RuntimeException ignored){}
            }
        }catch(IOException ex){storageAvailable=false;}
    }
    public synchronized void record(String level,String action,int status,double durationMs,boolean request) {
        var e=new Entry(++sequence,Instant.now(),level,action,status,Math.max(0,durationMs));
        entries.addLast(e);if(entries.size()>300)entries.removeFirst();
        if(request)observe(status,e.durationMs());
        try {
            if(Files.exists(file)&&Files.size(file)>524288)Files.move(file,file.resolveSibling("activity.previous.tsv"),StandardCopyOption.REPLACE_EXISTING);
            String line=e.id()+"\t"+e.time()+"\t"+e.level()+"\t"+e.action()+"\t"+e.status()+"\t"+e.durationMs()+"\n";
            Files.writeString(file,line,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);storageAvailable=true;
        }catch(IOException ex){storageAvailable=false;}
    }
    public synchronized List<Entry> entries(String level,String search,int limit) {
        String q=search==null?"":search.toLowerCase(Locale.ROOT);
        var list=new ArrayList<>(entries);Collections.reverse(list);
        return list.stream().filter(e->level==null||level.isBlank()||e.level().equals(level))
            .filter(e->e.action().toLowerCase(Locale.ROOT).contains(q)).limit(Math.max(1,Math.min(300,limit))).toList();
    }
    public synchronized void observe(int status,double millis) {requests++;if(status>=400)errors++;durations.addLast(Math.max(0,millis));if(durations.size()>100)durations.removeFirst();}
    public synchronized Map<String,Object> requests() {
        var sorted=durations.stream().sorted().toList();
        return Map.of("count",requests,"errors",errors,"sampleCount",sorted.size(),"averageMs",sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0),
            "p95Ms",sorted.isEmpty()?0:sorted.get(Math.max(0,(int)Math.ceil(sorted.size()*.95)-1)),"storageAvailable",storageAvailable);
    }
}
