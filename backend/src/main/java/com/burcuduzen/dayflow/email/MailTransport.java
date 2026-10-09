package com.burcuduzen.dayflow.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Component
public class MailTransport {
    private final JavaMailSenderImpl sender;
    private final String from;
    private final boolean configured;
    public MailTransport(@Value("${dayflow.mail.host:}") String host,
        @Value("${dayflow.mail.port:587}") int port,
        @Value("${dayflow.mail.username:}") String username,
        @Value("${dayflow.mail.password:}") String password,
        @Value("${dayflow.mail.from:}") String from,
        @Value("${dayflow.mail.ssl:false}") boolean ssl) {
        this.from = from.isBlank() ? username : from;
        configured = !host.isBlank() && !username.isBlank() && !password.isBlank() && !this.from.isBlank();
        sender = new JavaMailSenderImpl();
        sender.setHost(host); sender.setPort(port); sender.setUsername(username); sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        var properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.ssl.enable", Boolean.toString(ssl));
        properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(!ssl));
        properties.setProperty("mail.smtp.starttls.required", Boolean.toString(!ssl));
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "5000");
        properties.setProperty("mail.smtp.writetimeout", "5000");
    }
    public boolean configured() { return configured; }
    public void send(String recipient, String subject, String body) {
        if (!configured) throw new IllegalStateException("SMTP ayarları eksik.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(recipient); message.setSubject(subject); message.setText(body);
        sender.send(message);
    }
}
