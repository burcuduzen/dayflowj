package com.burcuduzen.dayflow.email;

import jakarta.persistence.*;
@Entity @Table(name = "email_preferences")
public class EmailPreferences {
    @Id public Long id = 1L;
    public boolean enabled;
    @Column(length = 254) public String recipient;
    public EmailPreferences() {}
}
