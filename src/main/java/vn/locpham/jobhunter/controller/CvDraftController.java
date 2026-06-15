package vn.locpham.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.CvDraft;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.cvdraft.ResCreateCvDraftDTO;
import vn.locpham.jobhunter.domain.reponse.cvdraft.ResFetchCvDraftDTO;
import vn.locpham.jobhunter.service.CvDraftService;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class CvDraftController {

    private final CvDraftService cvDraftService;

    public CvDraftController(CvDraftService cvDraftService) {
        this.cvDraftService = cvDraftService;
    }

    /**
     * POST /api/v1/cv-drafts — Lưu CV mới vào DB
     */
    @PostMapping("/cv-drafts")
    @ApiMessage("Save CV draft")
    public ResponseEntity<ResCreateCvDraftDTO> saveCvDraft(@Valid @RequestBody CvDraft reqDraft)
            throws IdInvalidException {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentUserEmail == null) {
            throw new IdInvalidException("Vui lòng đăng nhập để lưu CV");
        }
        CvDraft saved = this.cvDraftService.createCvDraft(reqDraft, currentUserEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.cvDraftService.convertToResCreateCvDraftDTO(saved));
    }

    /**
     * GET /api/v1/cv-drafts — Lấy danh sách CV của user hiện tại
     */
    @GetMapping("/cv-drafts")
    @ApiMessage("Fetch my CV drafts")
    public ResponseEntity<ResultPaginationDTO> getMyCvDrafts(Pageable pageable)
            throws IdInvalidException {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentUserEmail == null) {
            throw new IdInvalidException("Vui lòng đăng nhập");
        }
        return ResponseEntity.ok(this.cvDraftService.getCvDraftsByUser(currentUserEmail, pageable));
    }

    /**
     * GET /api/v1/cv-drafts/{id} — Lấy chi tiết 1 CV
     */
    @GetMapping("/cv-drafts/{id}")
    @ApiMessage("Fetch CV draft by id")
    public ResponseEntity<ResFetchCvDraftDTO> getCvDraftById(@PathVariable("id") Long id)
            throws IdInvalidException {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentUserEmail == null) {
            throw new IdInvalidException("Vui lòng đăng nhập");
        }
        CvDraft draft = this.cvDraftService.getCvDraftById(id, currentUserEmail);
        return ResponseEntity.ok(this.cvDraftService.convertToResFetchCvDraftDTO(draft));
    }

    /**
     * DELETE /api/v1/cv-drafts/{id} — Xóa CV
     */
    @DeleteMapping("/cv-drafts/{id}")
    @ApiMessage("Delete CV draft")
    public ResponseEntity<Void> deleteCvDraft(@PathVariable("id") Long id)
            throws IdInvalidException {
        String currentUserEmail = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentUserEmail == null) {
            throw new IdInvalidException("Vui lòng đăng nhập");
        }
        this.cvDraftService.deleteCvDraft(id, currentUserEmail);
        return ResponseEntity.ok(null);
    }
}
