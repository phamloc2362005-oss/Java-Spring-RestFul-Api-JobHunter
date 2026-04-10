package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.repository.SkillRepository;

@Service
public class SkillService {
    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public boolean isNameExist(String name) {
        return this.skillRepository.existsByName(name);
    }

    public Skill handleCreateNewSkill(Skill skill) {
        return this.skillRepository.save(skill);
    }

    public Skill fetchSkillById(long id) {
        Optional<Skill> skillOptional = this.skillRepository.findById(id);
        if (skillOptional.isPresent()) {
            return skillOptional.get();
        }
        return null;
    }

    public Skill handleUpdateSkill(Skill reqSkill) {
        Skill currentSkill = this.fetchSkillById(reqSkill.getId());
        if (currentSkill != null) {
            currentSkill.setName(reqSkill.getName());
            this.skillRepository.save(currentSkill);
        }
        return currentSkill;
    }

    public ResultPaginationDTO fetchAllSkill(Specification<Skill> spec, Pageable pageable) {
        Page<Skill> pageSkill = this.skillRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageSkill.getTotalPages());
        mt.setTotal(pageSkill.getTotalElements());

        List<Skill> listSkill = pageSkill.getContent();
        rs.setMeta(mt);
        rs.setResult(listSkill);
        return rs;
    }

    public void handleDeleteSkill(long id) {
        Optional<Skill> skillOptional = this.skillRepository.findById(id);
        Skill currentSkill = skillOptional.get();
        currentSkill.getJobs().forEach(job -> job.getSkills().remove(currentSkill));
        currentSkill.getSubscribers().forEach(sub -> sub.getSkills().remove(currentSkill));
        this.skillRepository.delete(currentSkill);
    }
}
