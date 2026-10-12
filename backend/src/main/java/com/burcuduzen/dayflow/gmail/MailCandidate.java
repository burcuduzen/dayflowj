package com.burcuduzen.dayflow.gmail;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name="mail_candidates", uniqueConstraints=@UniqueConstraint(columnNames={"account","messageId"}))
public class MailCandidate {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=254) public String account;
    @Column(nullable=false,length=100) public String messageId;
    @Column(length=200) public String title;
    @Column(length=500) public String reason;
    public OffsetDateTime startAt;
    public OffsetDateTime endAt;
    public Long eventId;
    @Column(length=20) public String status;
    protected MailCandidate() {}
    MailCandidate(String account,String messageId,MailEventParser.Draft draft) {
        this.account=account;this.messageId=messageId;this.title=draft.title();this.reason=draft.reason();
        this.startAt=draft.startAt();this.endAt=draft.endAt();this.status="PENDING";
    }
}
