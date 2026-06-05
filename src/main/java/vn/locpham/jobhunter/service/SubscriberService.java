package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.Subscriber;
import vn.locpham.jobhunter.domain.reponse.email.ResEmailJob;
import vn.locpham.jobhunter.repository.JobRepository;
import vn.locpham.jobhunter.repository.SkillRepository;
import vn.locpham.jobhunter.repository.SubscriberRepository;
import vn.locpham.jobhunter.util.SecurityUtils;

@Service
public class SubscriberService {
    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;
    private final JobRepository jobRepository;
    private final EmailService emailService;

    public SubscriberService(SubscriberRepository subscriberRepository, SkillRepository skillRepository,
            JobRepository jobRepository, EmailService emailService) {
        this.subscriberRepository = subscriberRepository;
        this.skillRepository = skillRepository;
        this.jobRepository = jobRepository;
        this.emailService = emailService;
    }

    public Subscriber findById(long id) {
        return subscriberRepository.findById(id).orElse(null);
    }

    public boolean existsByEmail(String email) {
        return subscriberRepository.existsByEmail(email);
    }

    public Subscriber createSubscriber(Subscriber subscriber) {
        if (subscriber.getSkills() != null) {
            List<Long> reqSkills = subscriber.getSkills().stream().map(x -> x.getId()).toList();
            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subscriber.setSkills(dbSkills);
        }
        return subscriberRepository.save(subscriber);
    }

    public Subscriber updateSubscriber(Subscriber subsDB, Subscriber subsRequest) {
        if (subsRequest.getSkills() != null) {
            List<Long> reqSkills = subsRequest.getSkills().stream().map(x -> x.getId()).toList();
            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            subsDB.setSkills(dbSkills);
        }
        return subscriberRepository.save(subsDB);
    }

    /**
     * Lấy subscriber của user đang đăng nhập (dựa theo email trong JWT)
     */
    public Subscriber getSubscriberByCurrentUser() {
        String email = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (email == null) return null;
        return subscriberRepository.findByEmail(email);
    }

    /**
     * Lấy subscriber theo email (dùng khi upsert)
     */
    public Subscriber getSubscriberByEmail(String email) {
        return subscriberRepository.findByEmail(email);
    }

    // convert Job -> DTO gửi mail
    public ResEmailJob convertToResEmailJob(Job job) {
        ResEmailJob res = new ResEmailJob();
        res.setName(job.getName());
        res.setSalary(job.getSalary());

        // company
        res.setCompany(new ResEmailJob.CompanyEmail(job.getCompany().getName()));

        // skill list
        List<Skill> skills = job.getSkills();
        List<ResEmailJob.SkillEmail> s = skills.stream().map(skill -> new ResEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());
        res.setSkills(s);
        return res;
    }

    // gửi mail job cho subscriber
    public void sendSubscriberEmailJobs() {
        List<Subscriber> subscribers = subscriberRepository.findAll();
        for (Subscriber subscriber : subscribers) {
            List<Skill> listSkills = subscriber.getSkills();
            if (listSkills != null && !listSkills.isEmpty()) {
                // tìm job theo skill
                List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
                if (listJobs != null && !listJobs.isEmpty()) {
                    List<ResEmailJob> arr = listJobs.stream().map(job -> this.convertToResEmailJob(job)).toList();

                    // gửi email
                    this.emailService.sendEmailFromTemplateSync(
                            subscriber.getEmail(),
                            "Cơ hội việc làm hot đang chờ đón bạn, khám phá ngay",
                            "job",
                            subscriber.getName(),
                            arr);
                }
            }
        }
    }
}
