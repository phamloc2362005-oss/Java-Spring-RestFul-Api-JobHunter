package vn.locpham.jobhunter.service;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final MailSender mailSender;

    public EmailService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail() {
        // Implement email sending logic using mailSender
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("phamloc2362005@gmail.com");
        msg.setSubject("Test Email");
        msg.setText("This is a test email sent from the EmailService.");
        mailSender.send(msg);
    }
}
