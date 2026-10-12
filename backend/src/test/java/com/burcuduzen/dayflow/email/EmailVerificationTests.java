package com.burcuduzen.dayflow.email;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationTests {
    @Test void connectingSendsTokenAndRequiresVerificationBeforeReminders() {
        var repository=mock(EmailPreferencesRepository.class);var mail=mock(MailTransport.class);var settings=new EmailPreferences();
        when(mail.configured()).thenReturn(true);when(repository.findById(1L)).thenReturn(Optional.of(settings));
        var service=new EmailVerificationService(repository,mail,"http://127.0.0.1:8080");
        service.connect("Owner@Example.com");
        assertFalse(settings.verified);assertFalse(settings.enabled);assertNull(settings.recipient);
        var body=ArgumentCaptor.forClass(String.class);verify(mail).send(eq("owner@example.com"),anyString(),body.capture());
        String token=body.getValue().split("token=")[1].split("\n")[0];assertEquals(EmailVerificationService.digest(token),settings.verificationHash);
        assertNotEquals(token,settings.verificationHash);
        service.verify(token);assertTrue(settings.verified);assertTrue(settings.enabled);assertEquals("owner@example.com",settings.recipient);
        assertNull(settings.verificationHash);assertThrows(ResponseStatusException.class,()->service.verify(token));
    }
    @Test void addressChangeKeepsCurrentRecipientUntilNewAddressIsVerified() {
        var repository=mock(EmailPreferencesRepository.class);var mail=mock(MailTransport.class);var settings=new EmailPreferences();
        settings.recipient="old@example.com";settings.verified=true;settings.enabled=true;
        when(repository.findById(1L)).thenReturn(Optional.of(settings));when(mail.configured()).thenReturn(true);
        new EmailVerificationService(repository,mail,"http://127.0.0.1:8080").connect("new@example.com");
        assertEquals("old@example.com",settings.recipient);assertEquals("new@example.com",settings.pendingRecipient);assertTrue(settings.enabled);
    }
    @Test void unavailableMailDoesNotWritePreferences() {
        var repository=mock(EmailPreferencesRepository.class);var mail=mock(MailTransport.class);
        var service=new EmailVerificationService(repository,mail,"http://127.0.0.1:8080");
        assertThrows(ResponseStatusException.class,()->service.connect("owner@example.com"));verifyNoInteractions(repository);
    }
    @Test void expiredAndIncorrectLinksAreRejected() {
        var repository=mock(EmailPreferencesRepository.class);var settings=new EmailPreferences();settings.pendingRecipient="owner@example.com";
        settings.verificationHash=EmailVerificationService.digest("token");settings.verificationExpires=OffsetDateTime.now().minusMinutes(1);
        when(repository.findById(1L)).thenReturn(Optional.of(settings));var service=new EmailVerificationService(repository,mock(MailTransport.class),"http://127.0.0.1:8080");
        assertThrows(ResponseStatusException.class,()->service.verify("token"));settings.verificationExpires=OffsetDateTime.now().plusMinutes(1);
        assertThrows(ResponseStatusException.class,()->service.verify("wrong"));assertFalse(settings.verified);
    }
    @Test void localPlannerOpensWithoutSessionButRejectsCrossOriginWrites() {
        var guard=new com.burcuduzen.dayflow.auth.AuthGuard();
        var request=mock(jakarta.servlet.http.HttpServletRequest.class);var response=mock(jakarta.servlet.http.HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/tasks");when(request.getMethod()).thenReturn("GET");assertTrue(guard.preHandle(request,response,new Object()));
        when(request.getMethod()).thenReturn("POST");when(request.getHeader("X-DayFlowJ-Request")).thenReturn("1");when(request.getHeader("Origin")).thenReturn("https://other.example");
        when(request.getServerName()).thenReturn("127.0.0.1");when(request.getServerPort()).thenReturn(8080);when(request.getScheme()).thenReturn("http");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,()->guard.preHandle(request,response,new Object())).getStatusCode());
    }
}
