package com.burcuduzen.dayflow.email;

import jakarta.persistence.*;
@Entity @Table(name = "email_preferences")
public class EmailPreferences {
    @Id public Long id = 1L;
    public boolean enabled;
    @Column(length = 254) public String recipient;
    @Column(nullable=false, columnDefinition="boolean default false") public boolean verified;
    @Column(length=254) public String pendingRecipient;
    @Column(length=64) public String verificationHash;
    public java.time.OffsetDateTime verificationExpires;
    public java.time.OffsetDateTime lastVerificationSent;
    public EmailPreferences() {}
}
