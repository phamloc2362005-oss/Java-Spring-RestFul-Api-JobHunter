package vn.locpham.jobhunter.controller;

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

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.Resume;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResCreateResumeDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResFetchResumeDTO;
import vn.locpham.jobhunter.domain.reponse.resume.ResUpdateResumeDTO;
import vn.locpham.jobhunter.service.ResumeService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
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

    @GetMapping("/resumes")
    @ApiMessage("Fetch all resumes")
    public ResponseEntity<ResultPaginationDTO> getAllResume(@Filter Specification<Resume> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(this.resumeService.fetchAllResume(spec, pageable));

    }
}
