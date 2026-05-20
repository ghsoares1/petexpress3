package com.petexpress.petexpress_site_backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String smtpHost;
    private final String smtpUsername;
    private final boolean emailEnabled;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.from:no-reply@petexpress.com}") String fromAddress,
                        @Value("${spring.mail.host:}") String smtpHost,
                        @Value("${spring.mail.username:}") String smtpUsername,
                        @Value("${app.email.enabled:false}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.smtpHost = smtpHost;
        this.smtpUsername = smtpUsername;
        this.emailEnabled = emailEnabled;
    }

    public void sendAccountCreatedEmail(String to, String name) {
        if (!isConfigured()) {
            logger.info("Envio de email desativado ou SMTP nao configurado. Cadastro concluido para {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("Bem-vindo a PetExpress!");
            helper.setText(buildAccountCreatedBody(name), true);

            mailSender.send(message);
            logger.info("Email de confirmacao de cadastro enviado para {}", to);
        } catch (MessagingException | MailException exception) {
            logger.warn("Falha ao enviar email de confirmacao de cadastro para {}. Cadastro permanece valido.", to, exception);
        }
    }

    private boolean isConfigured() {
        return emailEnabled
                && hasText(smtpHost)
                && hasText(smtpUsername)
                && !"smtp.example.com".equalsIgnoreCase(smtpHost)
                && !"your-smtp-username".equalsIgnoreCase(smtpUsername);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String buildAccountCreatedBody(String name) {
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<h2>Cadastro realizado com sucesso!</h2>");
        body.append("<p>Ola ").append(name).append(",</p>");
        body.append("<p>Seu cadastro na PetExpress foi concluido com sucesso.</p>");
        body.append("<p>Agora voce ja pode fazer login e comecar a comprar.</p>");
        body.append("<p>Obrigado por escolher a PetExpress!</p>");
        body.append("</body></html>");
        return body.toString();
    }
}
