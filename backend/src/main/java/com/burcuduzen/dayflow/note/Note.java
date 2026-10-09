package com.burcuduzen.dayflow.note;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "notes")
public class Note {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 200) public String title;
    @Column(nullable = false, length = 20000) public String content;
    @Column(nullable = false) public OffsetDateTime createdAt;
    @Column(nullable = false) public OffsetDateTime updatedAt;
    protected Note() {}
    public Note(String title, String content) {
        this.title = title; this.content = content;
        this.createdAt = OffsetDateTime.now(); this.updatedAt = this.createdAt;
    }
}
