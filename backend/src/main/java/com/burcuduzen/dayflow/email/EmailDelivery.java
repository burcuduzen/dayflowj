package com.burcuduzen.dayflow.email;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "email_delivery")
public class EmailDelivery {
    @Id @Column(length = 250) public String deliveryKey;
    public OffsetDateTime sentAt;
    public OffsetDateTime lastAttemptAt;
    protected EmailDelivery() {}
    public EmailDelivery(String key) { this.deliveryKey = key; }
}
