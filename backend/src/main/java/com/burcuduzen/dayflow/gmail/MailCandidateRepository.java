package com.burcuduzen.dayflow.gmail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MailCandidateRepository extends JpaRepository<MailCandidate,Long> {
    boolean existsByAccountAndMessageId(String account,String messageId);
    List<MailCandidate> findTop100ByAccountOrderByIdDesc(String account);
}
