package com.burcuduzen.dayflow.auth;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity @Table(name="local_account")
public class LocalAccount {
    @Id public Long id = 1L;
    @Column(nullable=false,length=254) public String email;
    @Column(nullable=false,length=300) public String passwordHash;
    public boolean verified;
    @Column(length=64) public String verificationHash;
    public OffsetDateTime verificationExpires;
    public OffsetDateTime lastVerificationSent;
    public LocalAccount() {}
}
