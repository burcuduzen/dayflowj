package com.burcuduzen.dayflow.email;

import com.burcuduzen.dayflow.task.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Component
public class EmailReminderJob {
    private static final Logger log = LoggerFactory.getLogger(EmailReminderJob.class);
    private final EmailPreferencesRepository preferences;
    private final EmailDeliveryRepository deliveries;
    private final TaskRepository tasks;
    private final MailTransport mail;
    public EmailReminderJob(EmailPreferencesRepository preferences, EmailDeliveryRepository deliveries, TaskRepository tasks, MailTransport mail) {
        this.preferences = preferences; this.deliveries = deliveries; this.tasks = tasks; this.mail = mail;
    }
    @Scheduled(fixedDelayString = "${dayflow.mail.poll-ms:60000}", initialDelay = 15000)
    public synchronized void check() {
        var settings = preferences.findById(1L).orElse(null);
        if (settings == null || !settings.enabled || settings.recipient == null || !mail.configured()) return;
        OffsetDateTime now = OffsetDateTime.now();
        for (Task task : tasks.findAllByOrderByCreatedAtDescIdDesc()) {
            if (task.isComplete() || !task.isReminderEnabled() || task.getDueDate() == null) continue;
            // Past tasks are included for one day; old overdue backlog never floods the inbox.
            long seconds = Duration.between(now.toInstant(), task.getDueDate().toInstant()).getSeconds();
            String stage = stage(seconds);
            if (stage == null) continue;
            String key = task.getId() + "|" + task.getDueDate().toInstant() + "|" + stage;
            var delivery = deliveries.findById(key).orElseGet(() -> new EmailDelivery(key));
            if (delivery.sentAt != null || delivery.lastAttemptAt != null && delivery.lastAttemptAt.isAfter(now.minusMinutes(15))) continue;
            delivery.lastAttemptAt = now; deliveries.saveAndFlush(delivery);
            // Recheck after scanning so completed or rescheduled tasks do not get stale reminders.
            var current = tasks.findById(task.getId()).orElse(null);
            if (current == null || current.isComplete() || !current.isReminderEnabled() || !Objects.equals(current.getDueDate(),task.getDueDate())) continue;
            String when = current.getDueDate().atZoneSameInstant(ZoneId.of(current.getTimeZone())).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm z"));
            String subject = "DayFlowJ · " + (stage.equals("DUE") ? "Görevin zamanı geldi" : "Yaklaşan görev");
            try {
                mail.send(settings.recipient, subject, current.getTitle() + "\n\nSon tarih: " + when + "\nÖncelik: " + current.getPriority()
                    + "\n\nGörevini DayFlowJ’de aç: http://127.0.0.1:8080/\nE-posta hatırlatmalarını uygulamadaki E-posta ayarlarından kapatabilirsin.");
                delivery.sentAt = OffsetDateTime.now(); deliveries.save(delivery);
            } catch (RuntimeException ex) {
                // Never log credentials, SMTP payloads or provider exception text.
                log.warn("DayFlowJ email reminder failed for task {}. Retry after 15 minutes.", current.getId());
            }
        }
    }
    public static String stage(long secondsUntilDue) {
        if (secondsUntilDue < -86400 || secondsUntilDue > 86400) return null;
        if (secondsUntilDue <= 0) return "DUE";
        if (secondsUntilDue <= 3600) return "HOUR";
        return "DAY";
    }
}
