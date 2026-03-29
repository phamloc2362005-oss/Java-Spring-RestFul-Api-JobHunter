package vn.locpham.jobhunter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.locpham.jobhunter.service.EmailService;
import vn.locpham.jobhunter.service.SubscriberService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;

@RestController
@RequestMapping("/api/v1")
public class EmailController {
    private final EmailService emailService;
    private final SubscriberService subscriberService;

    public EmailController(EmailService emailService, SubscriberService subscriberService) {
        this.emailService = emailService;
        this.subscriberService = subscriberService;
    }

    @GetMapping("/email")
    @ApiMessage("Send simple email")
    public String sendEmail() {
        this.subscriberService.sendSubscriberEmailJobs();
        return "ok";
    }
}
