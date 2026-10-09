package com.burcuduzen.dayflow.auth;
import com.burcuduzen.dayflow.email.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountTests {
    @Test void passwordsUseSaltAndRejectWrongValue() {
        String first=PasswordHasher.hash("correct-password-123");
        assertTrue(PasswordHasher.matches("correct-password-123",first));
        assertFalse(PasswordHasher.matches("different-password",first));
        assertFalse(PasswordHasher.matches("correct-password-123","invalid"));
        assertNotEquals(first,PasswordHasher.hash("correct-password-123"));
    }
    @Test void registrationSendsTokenButStoresOnlyHashesAndUsesAccountEmail() {
        var accounts=mock(AccountRepository.class);var preferences=mock(EmailPreferencesRepository.class);var mail=mock(MailTransport.class);
        when(mail.configured()).thenReturn(true);when(preferences.findById(1L)).thenReturn(Optional.empty());
        var service=new AccountService(accounts,preferences,mail,"http://127.0.0.1:8080");
        service.register("Owner@Example.com","correct-password-123",true);
        var captured=ArgumentCaptor.forClass(LocalAccount.class);verify(accounts).saveAndFlush(captured.capture());
        var account=captured.getValue();assertEquals("owner@example.com",account.email);assertFalse(account.verified);
        assertTrue(PasswordHasher.matches("correct-password-123",account.passwordHash));
        var body=ArgumentCaptor.forClass(String.class);verify(mail).send(eq(account.email),anyString(),body.capture());
        String token=body.getValue().split("token=")[1].split("\n")[0];
        assertEquals(PasswordHasher.digest(token),account.verificationHash);assertNotEquals(token,account.verificationHash);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        service.verify(token);assertTrue(account.verified);assertNull(account.verificationHash);
        assertThrows(ResponseStatusException.class,()->service.verify(token));
    }
    @Test void missingSenderDoesNotCreateAnAccount() {
        var accounts=mock(AccountRepository.class);var preferences=mock(EmailPreferencesRepository.class);var mail=mock(MailTransport.class);
        var service=new AccountService(accounts,preferences,mail,"http://127.0.0.1:8080");
        assertThrows(ResponseStatusException.class,()->service.register("owner@example.com","correct-password-123",true));
        verify(accounts,never()).saveAndFlush(any());verifyNoInteractions(preferences);
    }
    @Test void expiredOrWrongVerificationCannotVerifyAccount() {
        var accounts=mock(AccountRepository.class);var account=new LocalAccount();
        account.verificationHash=PasswordHasher.digest("original-token");account.verificationExpires=OffsetDateTime.now().minusSeconds(1);
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        var service=new AccountService(accounts,mock(EmailPreferencesRepository.class),mock(MailTransport.class),"http://127.0.0.1:8080");
        assertThrows(ResponseStatusException.class,()->service.verify("original-token"));
        account.verificationExpires=OffsetDateTime.now().plusMinutes(10);
        assertThrows(ResponseStatusException.class,()->service.verify("different-token"));assertFalse(account.verified);
    }
    @Test void verifiedSessionIsRequiredAndCrossOriginWritesAreRejected() {
        var service=mock(AccountService.class);var guard=new AuthGuard(service);
        var request=mock(jakarta.servlet.http.HttpServletRequest.class);var response=mock(jakarta.servlet.http.HttpServletResponse.class);
        when(request.getRequestURI()).thenReturn("/api/tasks");when(request.getMethod()).thenReturn("GET");
        var error=assertThrows(ResponseStatusException.class,()->guard.preHandle(request,response,new Object()));
        assertEquals(HttpStatus.UNAUTHORIZED,error.getStatusCode());
        var session=mock(jakarta.servlet.http.HttpSession.class);when(request.getSession(false)).thenReturn(session);when(session.getAttribute("accountId")).thenReturn(1L);
        var account=new LocalAccount();account.verified=true;when(service.current()).thenReturn(account);
        assertTrue(guard.preHandle(request,response,new Object()));
        when(request.getMethod()).thenReturn("POST");when(request.getHeader("X-DayFlowJ-Request")).thenReturn("1");
        when(request.getHeader("Origin")).thenReturn("https://other.example");when(request.getServerName()).thenReturn("127.0.0.1");when(request.getServerPort()).thenReturn(8080);when(request.getScheme()).thenReturn("http");
        assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,()->guard.preHandle(request,response,new Object())).getStatusCode());
    }
}
