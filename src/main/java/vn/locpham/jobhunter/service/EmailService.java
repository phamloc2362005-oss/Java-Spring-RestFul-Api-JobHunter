package vn.locpham.jobhunter.service;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.Resume;
import vn.locpham.jobhunter.domain.Subscriber;
import vn.locpham.jobhunter.repository.JobRepository;

@Service
public class EmailService {
    private final MailSender mailSender;
    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;
    private final JobRepository jobRepository;

    public EmailService(MailSender mailSender, JavaMailSender javaMailSender, TemplateEngine templateEngine,
            JobRepository jobRepository) {
        this.mailSender = mailSender;
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.jobRepository = jobRepository;
    }

    // public void sendEmail() {
    // // Implement email sending logic using mailSender
    // SimpleMailMessage msg = new SimpleMailMessage();
    // msg.setTo("phamloc2362005@gmail.com");
    // msg.setSubject("Test Email");
    // msg.setText("This is a test email sent from the EmailService.");
    // mailSender.send(msg);
    // }
    // hàm gửi mail cơ bản
    public void sendEmailSync(String to, String subject, String content, boolean isMultipart,
            boolean isHtml) {
        // Prepare message using a Spring helper
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage,
                    isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content, isHtml);
            this.javaMailSender.send(mimeMessage);
        } catch (MailException | MessagingException e) {
            System.out.println("ERROR SEND EMAIL: " + e);
        }
    }

    // mail gửi job cho người đăng kí
    @Async
    public void sendEmailFromTemplateSync(String to, String subject, String templateName, String username,
            Object value) {
        Context context = new Context();
        context.setVariable("name", username);
        context.setVariable("jobs", value);
        String content = this.templateEngine.process(templateName, context);
        this.sendEmailSync(to, subject, content, false, true);
    }

    // mail gửi otp
    @Async
    public void sendOtpEmail(String to, String subject, String templateName, String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        String content = this.templateEngine.process(templateName, context);
        this.sendEmailSync(to, subject, content, false, true);
    }

    // ================================================================
    // mail thông báo status resume cho ứng viên
    // ================================================================
    @Async
    public void sendResumeStatusEmail(Resume resume) {
        if (resume == null || resume.getEmail() == null)
            return;

        String status = resume.getStatus() != null ? resume.getStatus().name() : "PENDING";

        // Subject động theo status
        String subject;
        switch (status) {
            case "APPROVED" -> subject = "[JobHunter] Your application has been Approved!";
            case "REVIEWING" -> subject = "[JobHunter] Your application is Under Review";
            case "REJECTED" -> subject = "[JobHunter] Application Status Update";
            default -> subject = "[JobHunter] Application Status Update";
        }

        // Build Thymeleaf context
        Context ctx = new Context();
        ctx.setVariable("candidateName",
                resume.getUser() != null && resume.getUser().getName() != null
                        ? resume.getUser().getName()
                        : "Candidate");
        ctx.setVariable("status", status);
        ctx.setVariable("jobName",
                resume.getJob() != null ? resume.getJob().getName() : "N/A");
        ctx.setVariable("companyName",
                resume.getJob() != null && resume.getJob().getCompany() != null
                        ? resume.getJob().getCompany().getName()
                        : "N/A");
        ctx.setVariable("jobLocation",
                resume.getJob() != null ? resume.getJob().getLocation() : null);

        // Format salary
        if (resume.getJob() != null && resume.getJob().getSalary() > 0) {
            NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
            ctx.setVariable("salary", fmt.format(resume.getJob().getSalary()) + " VND");
        } else {
            ctx.setVariable("salary", null);
        }

        ctx.setVariable("aiScore", resume.getAiScore());
        ctx.setVariable("appUrl", "http://localhost:5173/job");

        String content = this.templateEngine.process("resume-status", ctx);
        this.sendEmailSync(resume.getEmail(), subject, content, false, true);
    }

    // ================================================================
    // mail thông báo job mới cho subscriber có skill trùng
    // ================================================================
    @Async
    public void sendNewJobNotification(Subscriber subscriber, Job job) {
        if (subscriber == null || subscriber.getEmail() == null || job == null) return;

        Context ctx = new Context();
        ctx.setVariable("name", subscriber.getName() != null ? subscriber.getName() : "bạn");
        ctx.setVariable("job", job);

        if (job.getSalary() > 0) {
            NumberFormat fmt = NumberFormat.getNumberInstance(Locale.US);
            ctx.setVariable("salary", fmt.format(job.getSalary()) + " VND");
        } else {
            ctx.setVariable("salary", "Thỏa thuận");
        }

        ctx.setVariable("appUrl", "http://localhost:5173/job");

        String content = this.templateEngine.process("new-job-notification", ctx);
        this.sendEmailSync(
            subscriber.getEmail(),
            "[JobHunter] Việc làm mới phù hợp với bạn: " + job.getName(),
            content, false, true
        );
    }

}
