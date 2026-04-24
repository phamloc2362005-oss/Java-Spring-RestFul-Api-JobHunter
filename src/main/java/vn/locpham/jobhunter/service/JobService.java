package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Expertise;
import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.job.ResCreateJobDTO;
import vn.locpham.jobhunter.domain.reponse.job.ResUpdateJobDTO;
import vn.locpham.jobhunter.repository.CompanyRepository;
import vn.locpham.jobhunter.repository.ExpertiseRepository;
import vn.locpham.jobhunter.repository.JobRepository;
import vn.locpham.jobhunter.repository.SkillRepository;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;
    private final ExpertiseRepository expertiseRepository;
    private final ExpertiseService expertiseService;

    public JobService(JobRepository jobRepository, SkillRepository skillRepository,
            CompanyRepository companyRepository, ExpertiseRepository expertiseRepository,
            ExpertiseService expertiseService) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.companyRepository = companyRepository;
        this.expertiseRepository = expertiseRepository;
        this.expertiseService = expertiseService;
    }

    public Job fetchJobById(long id) {
        Optional<Job> job = this.jobRepository.findById(id);
        if (job.isPresent()) {
            return job.get();
        }
        return null;
    }

    public ResCreateJobDTO handleCreateJob(Job job) {
        if (job.getSkills() != null) {
            List<Long> reqSkills = job.getSkills().stream().map(x -> x.getId()).collect(Collectors.toList());
            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            job.setSkills(dbSkills);
        }
        if (job.getExpertise() != null) {
            Expertise dbExpertises = this.expertiseService.fetchExpertiseById(job.getExpertise().getId());
            job.setExpertise(dbExpertises);
        }

        if (job.getCompany() != null) {
            Optional<Company> comOptional = this.companyRepository.findById(job.getCompany().getId());
            if (comOptional.isPresent()) {
                job.setCompany(comOptional.get());
            }
        }
        Job currentJob = this.jobRepository.save(job);
        ResCreateJobDTO res = new ResCreateJobDTO();

        if (currentJob.getName() != null) {
            res.setName(currentJob.getName());
        }

        res.setActive(currentJob.isActive()); // boolean primitive → không null

        if (currentJob.getLocation() != null) {
            res.setLocation(currentJob.getLocation());
        }

        if (currentJob.getLevel() != null) {
            res.setLevel(currentJob.getLevel());
        }

        if (currentJob.getQuantity() != 0) {
            res.setQuantity(currentJob.getQuantity());
        }

        if (currentJob.getSalary() != 0) {
            res.setSalary(currentJob.getSalary());
        }

        if (currentJob.getEndDate() != null) {
            res.setEndDate(currentJob.getEndDate());
        }

        if (currentJob.getStartDate() != null) {
            res.setStartDate(currentJob.getStartDate());
        }

        if (currentJob.getCreatedAt() != null) {
            res.setCreatedAt(currentJob.getCreatedAt());
        }

        if (currentJob.getCreatedBy() != null) {
            res.setCreatedBy(currentJob.getCreatedBy());
        }
        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills().stream().map(x -> x.getName()).collect(Collectors.toList());
            res.setSkills(skills);
        }
        if (currentJob.getExpertise() != null) {
            String dbExpertises = this.expertiseService.fetchExpertiseById(currentJob.getExpertise().getId()).getName();
            res.setExpertise(dbExpertises);
        }

        return res;
    }

    public ResUpdateJobDTO handleUpdateJob(Job reqJob, Job jobInDB) {
        if (reqJob.getSkills() != null) {
            List<Long> reqSkills = reqJob.getSkills().stream().map(x -> x.getId()).collect(Collectors.toList());
            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            jobInDB.setSkills(dbSkills);
        }

        if (reqJob.getExpertise() != null) {
            Expertise dbExpertises = this.expertiseService.fetchExpertiseById(reqJob.getExpertise().getId());
            jobInDB.setExpertise(dbExpertises);
        }
        if (reqJob.getCompany() != null) {
            Optional<Company> comOptional = this.companyRepository.findById(reqJob.getCompany().getId());
            if (comOptional.isPresent()) {
                jobInDB.setCompany(comOptional.get());
            }
        }

        if (reqJob.getName() != null) {
            jobInDB.setName(reqJob.getName());
        }

        if (reqJob.getSalary() != 0) {
            jobInDB.setSalary(reqJob.getSalary());
        }

        if (reqJob.getQuantity() != 0) {
            jobInDB.setQuantity(reqJob.getQuantity());
        }

        if (reqJob.getLocation() != null) {
            jobInDB.setLocation(reqJob.getLocation());
        }

        if (reqJob.getDescription() != null) {
            jobInDB.setDescription(reqJob.getDescription());
        }

        if (reqJob.getRequired() != null) {
            jobInDB.setRequired(reqJob.getRequired());
        }

        if (reqJob.getBenefit() != null) {
            jobInDB.setBenefit(reqJob.getBenefit());
        }

        if (reqJob.getLevel() != null) {
            jobInDB.setLevel(reqJob.getLevel());
        }

        if (reqJob.getStartDate() != null) {
            jobInDB.setStartDate(reqJob.getStartDate());
        }

        if (reqJob.getEndDate() != null) {
            jobInDB.setEndDate(reqJob.getEndDate());
        }

        jobInDB.setActive(reqJob.isActive());

        Job currentJob = this.jobRepository.save(jobInDB);
        ResUpdateJobDTO res = new ResUpdateJobDTO();
        res.setName(currentJob.getName());
        res.setActive(currentJob.isActive());
        res.setLevel(currentJob.getLevel());
        res.setLocation(currentJob.getLocation());
        res.setQuantity(currentJob.getQuantity());
        res.setSalary(currentJob.getSalary());
        res.setEndDate(currentJob.getEndDate());
        res.setStartDate(currentJob.getStartDate());
        res.setUpdatedAt(currentJob.getUpdatedAt());
        res.setUpdatedBy(currentJob.getUpdatedBy());
        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills().stream().map(x -> x.getName()).collect(Collectors.toList());
            res.setSkills(skills);
        }

        if (currentJob.getExpertise() != null) {
            String dbExpertises = this.expertiseService.fetchExpertiseById(currentJob.getExpertise().getId()).getName();
            res.setExpertise(dbExpertises);
        }
        return res;
    }

    public void handleDeleteJob(long id) {
        this.jobRepository.deleteById(id);
    }

    public ResultPaginationDTO fetchAllJobs(Specification<Job> spec, Pageable pageable) {
        Page<Job> pageJobs = this.jobRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageJobs.getTotalPages());
        mt.setTotal(pageJobs.getTotalElements());
        List<Job> jobs = pageJobs.getContent();
        rs.setMeta(mt);
        rs.setResult(jobs);
        return rs;
    }

}
