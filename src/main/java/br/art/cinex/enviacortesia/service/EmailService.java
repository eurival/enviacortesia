package br.art.cinex.enviacortesia.service;


import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    public void enviarEmailComAnexo(String destinatario, String assunto, String texto, 
                                   ByteArrayOutputStream anexo, String nomeAnexo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(texto);

            ByteArrayResource resource = new ByteArrayResource(anexo.toByteArray());
            helper.addAttachment(nomeAnexo, resource);

            mailSender.send(message);
            logger.info("Email enviado com sucesso para: {}", destinatario);

        } catch (Exception e) {
            logger.error("Erro ao enviar email para: {}", destinatario, e);
            throw new RuntimeException("Falha ao enviar email", e);
        }
    }
}
