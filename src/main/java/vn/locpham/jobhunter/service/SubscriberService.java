package vn.locpham.jobhunter.service;

import java.util.List;

import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.Subscriber;
import vn.locpham.jobhunter.repository.SkillRepository;
import vn.locpham.jobhunter.repository.SubscriberRepository;

@Service
public class SubscriberService {
    private final SubscriberRepository subscriberRepository;
    private final SkillRepository skillRepository;

    public SubscriberService(SubscriberRepository subscriberRepository, SkillRepository skillRepository) {
        this.subscriberRepository = subscriberRepository;
        this.skillRepository = skillRepository;
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
}
