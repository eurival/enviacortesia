package br.art.cinex.enviacortesia.service;



import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;

import org.springframework.stereotype.Service;

import java.util.Properties;
import java.io.ByteArrayOutputStream;
@Service
public class JavaMailSender {

    public static void sendEmail(String to, String subject, String body, ByteArrayOutputStream pdfStream, String attachmentName) {
        final String username = "gedtotal@gmail.com";
        final String password = "cqvnvtjfhnqrekco";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Corpo do email
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);

            // Criação do multipart
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            // Adicionando o anexo do PDF
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource dataSource = new ByteArrayDataSource(pdfStream.toByteArray(), "application/pdf");
            attachmentPart.setDataHandler(new DataHandler(dataSource));
            attachmentPart.setFileName(attachmentName);
            multipart.addBodyPart(attachmentPart);

            // Setando o conteúdo do email
            message.setContent(multipart);

            // Enviando o email
            Transport.send(message);

            System.out.println("E-mail enviado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage(), e);
        }
    }
}
