package com.burcuduzen.dayflow.task;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) { super("Görev bulunamadı: " + id); }
}
