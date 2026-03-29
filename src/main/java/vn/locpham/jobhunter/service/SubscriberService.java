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

    public ResEmailJob convertToResEmailJob(Job job) {
        ResEmailJob res = new ResEmailJob();
        res.setName(job.getName());
        res.setSalary(job.getSalary());
        res.setCompany(new ResEmailJob.CompanyEmail(job.getCompany().getName()));
        List<Skill> skills = job.getSkills();
        List<ResEmailJob.SkillEmail> s = skills.stream().map(skill -> new ResEmailJob.SkillEmail(skill.getName()))
                .collect(Collectors.toList());
        res.setSkills(s);
        return res;
    }

    public void sendSubscriberEmailJobs() {
        List<Subscriber> subscribers = subscriberRepository.findAll();
        for (Subscriber subscriber : subscribers) {
            List<Skill> listSkills = subscriber.getSkills();
            if (listSkills != null && !listSkills.isEmpty()) {
                List<Job> listJobs = this.jobRepository.findBySkillsIn(listSkills);
                if (listJobs != null && !listJobs.isEmpty()) {
                    List<ResEmailJob> arr = listJobs.stream().map(job -> this.convertToResEmailJob(job)).toList();
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
