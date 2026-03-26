package vn.locpham.jobhunter.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.catalina.security.SecurityUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;
import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.domain.Resume;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResCreateResumeDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResFetchResumeDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResUpdateResumeDTO;
import vn.locpham.jobhunter.service.ResumeService;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;
    private final FilterBuilder filterBuilder;
    private final FilterSpecificationConverter filterSpecificationConverter;

    public ResumeController(ResumeService resumeService, UserService userService,
            FilterBuilder filterBuilder,
            FilterSpecificationConverter filterSpecificationConverter) {
        this.resumeService = resumeService;
        this.userService = userService;
        this.filterBuilder = filterBuilder;
        this.filterSpecificationConverter = filterSpecificationConverter;
    }

    @PostMapping("/resumes")
    @ApiMessage("Create a resume")
    public ResponseEntity<ResCreateResumeDTO> createNewResume(@Valid @RequestBody Resume reqResume)
            throws IdInvalidException {
        Resume resume = this.resumeService.handleCreateNewResume(reqResume);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.resumeService.convertToResCreateResumeDTO(resume));
    }

    @PutMapping("resumes")
    @ApiMessage("Create a resume")
    public ResponseEntity<ResUpdateResumeDTO> updateResume(@RequestBody Resume reqResume)
            throws IdInvalidException {
        Resume resume = this.resumeService.handleUpdateResume(reqResume);
        if (resume == null) {
            throw new IdInvalidException("Resume với id = " + resume.getId() + " không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(this.resumeService.convertToResUpdateResumeDTO(resume));
    }

    @DeleteMapping("/resumes/{id}")
    @ApiMessage("Delete a resume")
    public ResponseEntity<Void> deleteResume(@PathVariable("id") Long id) throws IdInvalidException {
        Resume resume = this.resumeService.fetchResumeById(id);
        if (resume == null) {
            throw new IdInvalidException("Resume với id = " + id + " không tồn tại");
        }
        this.resumeService.handleDeleteResume(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("resumes/{id}")
    @ApiMessage("Fetch a resume")
    public ResponseEntity<ResFetchResumeDTO> getResumeById(@PathVariable("id") long id) throws IdInvalidException {
        Resume resume = this.resumeService.fetchResumeById(id);
        if (resume == null) {
            throw new IdInvalidException("Resume với id = " + id + " không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(this.resumeService.convertResFetchResumeDTO(resume));
    }

    // @GetMapping("/resumes")
    // @ApiMessage("Fetch all resumes")
    // public ResponseEntity<ResultPaginationDTO> getAllResume(@Filter
    // Specification<Resume> spec, Pageable pageable) {
    // return
    // ResponseEntity.status(HttpStatus.OK).body(this.resumeService.fetchAllResume(spec,
    // pageable));

    // }

    @GetMapping("/resumes")
    @ApiMessage("Fetch all resume with paginate")
    public ResponseEntity<ResultPaginationDTO> fetchAll(
            @Filter Specification<Resume> spec,
            Pageable pageable) {

        List<Long> arrJobIds = null;
        String email = SecurityUtils.getCurrentUserLogin().isPresent() == true
                ? SecurityUtils.getCurrentUserLogin().get()
                : "";
        User currentUser = this.userService.handleGetUserByUsername(email);
        if (currentUser != null) {
            Company userCompany = currentUser.getCompany();
            if (userCompany != null) {
                List<Job> companyJobs = userCompany.getJobs();
                if (companyJobs != null && companyJobs.size() > 0) {
                    arrJobIds = companyJobs.stream().map(x -> x.getId())
                            .collect(Collectors.toList());
                }
            }
        }

        Specification<Resume> jobInSpec = filterSpecificationConverter.convert(filterBuilder.field("job")
                .in(filterBuilder.input(arrJobIds)).get());

        Specification<Resume> finalSpec = jobInSpec.and(spec);

        return ResponseEntity.ok().body(this.resumeService.fetchAllResume(finalSpec, pageable));
    }

    @PostMapping("/resumes/by-user")
    @ApiMessage("Get list resumes by user")
    public ResponseEntity<ResultPaginationDTO> fetchResumeByUser(Pageable pageable) {

        return ResponseEntity.ok().body(this.resumeService.fetchResumeByUser(pageable));
    }

}
