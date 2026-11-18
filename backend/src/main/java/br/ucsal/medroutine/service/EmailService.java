package br.ucsal.medroutine.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String remetente;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${medroutine.mail.remetente}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    public void enviarCodigoRecuperacao(String destino, String nome, String codigo) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP não configurado. Código de recuperação para {}: {}", destino, codigo);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destino);
        msg.setSubject("MedRoutine – código para redefinir sua senha");
        msg.setText("Olá, " + nome + ".\n\nSeu código para redefinir a senha do MedRoutine é: " + codigo
                + "\nEle vale por 30 minutos.\n\nSe você não pediu a redefinição, ignore este e-mail.");
        try {
            sender.send(msg);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar e-mail de recuperação para {}", destino, e);
        }
    }
}
