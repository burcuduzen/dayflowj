package com.burcuduzen.dayflow.email;

import com.burcuduzen.dayflow.task.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailReminderTests {
    @Test void deadlinesUseRelevantStageOnly() {
        assertNull(EmailReminderJob.stage(86401));
        assertEquals("DAY", EmailReminderJob.stage(7200));
        assertEquals("HOUR", EmailReminderJob.stage(1800));
        assertEquals("DUE", EmailReminderJob.stage(0));
        assertNull(EmailReminderJob.stage(-86401));
    }
    @Test void alreadySentReminderIsNotSentAgain() {
        var preferences = mock(EmailPreferencesRepository.class);
        var deliveries = mock(EmailDeliveryRepository.class);
        var tasks = mock(TaskRepository.class);
        var mail = mock(MailTransport.class);
        var settings = new EmailPreferences(); settings.enabled = true; settings.recipient = "user@example.com";
        when(preferences.findById(1L)).thenReturn(Optional.of(settings)); when(mail.configured()).thenReturn(true);
        var task = mock(Task.class); var due = OffsetDateTime.now().plusHours(2);
        when(task.getId()).thenReturn(1L); when(task.getDueDate()).thenReturn(due); when(task.isReminderEnabled()).thenReturn(true);
        when(tasks.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(task));
        var sent = new EmailDelivery("1|" + due.toInstant() + "|DAY"); sent.sentAt = OffsetDateTime.now();
        when(deliveries.findById(anyString())).thenReturn(Optional.of(sent));
        new EmailReminderJob(preferences, deliveries, tasks, mail).check();
        verify(mail, never()).send(anyString(), anyString(), anyString());
    }
    @Test void successfulDeliveryIsRecordedAndNextCheckSkipsIt() {
        var preferences = mock(EmailPreferencesRepository.class);
        var deliveries = mock(EmailDeliveryRepository.class);
        var tasks = mock(TaskRepository.class);
        var mail = mock(MailTransport.class);
        var settings = new EmailPreferences(); settings.enabled = true; settings.recipient = "user@example.com";
        when(preferences.findById(1L)).thenReturn(Optional.of(settings)); when(mail.configured()).thenReturn(true);
        var task = mock(Task.class); var due = OffsetDateTime.now().plusHours(2);
        when(task.getId()).thenReturn(1L); when(task.getDueDate()).thenReturn(due);
        when(task.getTimeZone()).thenReturn("Europe/Istanbul"); when(task.getTitle()).thenReturn("Sunum");
        when(task.isReminderEnabled()).thenReturn(true);
        when(tasks.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(task));
        when(tasks.findById(1L)).thenReturn(Optional.of(task));
        var delivery = new EmailDelivery("1|" + due.toInstant() + "|DAY");
        when(deliveries.findById(anyString())).thenReturn(Optional.of(delivery));
        var job = new EmailReminderJob(preferences, deliveries, tasks, mail);
        job.check(); job.check();
        assertNotNull(delivery.sentAt);
        verify(mail, times(1)).send(eq(settings.recipient), anyString(), contains("Sunum"));
        verify(deliveries).save(delivery);
    }
    @Test void disabledPreferencesNeverSendMail() {
        var preferences = mock(EmailPreferencesRepository.class);
        var deliveries = mock(EmailDeliveryRepository.class);
        var tasks = mock(TaskRepository.class);
        var mail = mock(MailTransport.class);
        when(preferences.findById(1L)).thenReturn(Optional.of(new EmailPreferences()));
        new EmailReminderJob(preferences, deliveries, tasks, mail).check();
        verifyNoInteractions(mail, tasks);
    }
}
