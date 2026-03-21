package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.Resume;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResCreateResumeDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResFetchResumeDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResUpdateResumeDTO;
import vn.locpham.jobhunter.repository.ResumeRepository;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@Service
public class ResumeService {

    @Autowired
    FilterBuilder fb;
    @Autowired
    private FilterParser filterParser;

    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;

    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final JobService jobService;

    public ResumeService(ResumeRepository resumeRepository, UserService userService,
            JobService jobService) {
        this.resumeRepository = resumeRepository;
        this.userService = userService;
        this.jobService = jobService;
    }

    public Resume fetchResumeById(long id) {
        Optional<Resume> resOptional = this.resumeRepository.findById(id);
        return resOptional.isPresent() ? resOptional.get() : null;
    }

    public Resume handleCreateNewResume(Resume resume) throws IdInvalidException {
        User user = resume.getUser();
        Job job = resume.getJob();
        if (this.userService.fetchUserById(user.getId()) == null || this.jobService.fetchJobById(job.getId()) == null) {
            throw new IdInvalidException("User/Job voi id tai len khong ton tai");
        }
        return this.resumeRepository.save(resume);
    }

    public Resume handleUpdateResume(Resume reqResume) {
        Resume resume = this.fetchResumeById(reqResume.getId());
        if (resume != null) {
            resume.setStatus(reqResume.getStatus());
        }
        return this.resumeRepository.save(resume);
    }

    public void handleDeleteResume(long id) {
        this.resumeRepository.deleteById(id);
    }

    public ResultPaginationDTO fetchAllResume(Specification<Resume> spec, Pageable pageable) {
        Page<Resume> pageResumes = this.resumeRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageResumes.getTotalPages());
        mt.setTotal(pageResumes.getTotalElements());

        List<ResFetchResumeDTO> listResume = pageResumes.getContent().stream()
                .map(item -> new ResFetchResumeDTO(
                        item.getId(),
                        item.getEmail(),
                        item.getUrl(),
                        item.getStatus(),
                        item.getCreatedAt(),
                        item.getUpdatedAt(),
                        item.getCreatedBy(),
                        item.getUpdatedBy(),
                        new ResFetchResumeDTO.UserResume(
                                item.getUser() != null ? item.getUser().getId() : 0,
                                item.getUser() != null ? item.getUser().getName() : null),
                        new ResFetchResumeDTO.JobResume(
                                item.getJob() != null ? item.getJob().getId() : 0,
                                item.getJob() != null ? item.getJob().getName() : null)))
                .collect(Collectors.toList());

        rs.setMeta(mt);
        rs.setResult(listResume);
        return rs;
    }

    public ResUpdateResumeDTO convertToResUpdateResumeDTO(Resume resume) {
        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        return res;
    }

    public ResCreateResumeDTO convertToResCreateResumeDTO(Resume resume) {
        ResCreateResumeDTO res = new ResCreateResumeDTO();
        res.setId(resume.getId());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        return res;
    }

    public ResFetchResumeDTO convertResFetchResumeDTO(Resume resume) {
        ResFetchResumeDTO res = new ResFetchResumeDTO();
        ResFetchResumeDTO.UserResume user = new ResFetchResumeDTO.UserResume();
        ResFetchResumeDTO.JobResume job = new ResFetchResumeDTO.JobResume();
        if (resume.getUser() != null) {
            user.setId(resume.getUser().getId());
            user.setName(resume.getUser().getName());
            res.setUser(user);
        }
        if (resume.getJob() != null) {
            job.setId(resume.getJob().getId());
            job.setName(resume.getJob().getName());
            res.setJob(job);
            res.setCompany(resume.getJob().getCompany().getName());
        }

        res.setId(resume.getId());
        res.setEmail(resume.getEmail());
        res.setUrl(resume.getUrl());
        res.setStatus(resume.getStatus());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        return res;
    }

    public ResultPaginationDTO fetchResumeByUser(Pageable pageable) {
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        FilterNode node = filterParser.parse("email='" + email + "'");
        FilterSpecification<Resume> spec = filterSpecificationConverter.convert(node);
        Page<Resume> pageResumes = this.resumeRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageResumes.getTotalPages());
        mt.setTotal(pageResumes.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageResumes.getContent());
        return rs;
    }
}
