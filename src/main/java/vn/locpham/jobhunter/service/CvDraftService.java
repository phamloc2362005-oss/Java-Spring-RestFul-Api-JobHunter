package vn.locpham.jobhunter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.CvDraft;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.cvdraft.ResCreateCvDraftDTO;
import vn.locpham.jobhunter.domain.reponse.cvdraft.ResFetchCvDraftDTO;
import vn.locpham.jobhunter.repository.CvDraftRepository;
import vn.locpham.jobhunter.util.error.IdInvalidException;

import java.util.Optional;

@Service
public class CvDraftService {

    private final CvDraftRepository cvDraftRepository;
    private final UserService userService;

    public CvDraftService(CvDraftRepository cvDraftRepository, UserService userService) {
        this.cvDraftRepository = cvDraftRepository;
        this.userService = userService;
    }

    /**
     * Tạo CV draft mới, gán cho user đang đăng nhập.
     */
    public CvDraft createCvDraft(CvDraft draft, String userEmail) throws IdInvalidException {
        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("Không tìm thấy user với email: " + userEmail);
        }
        draft.setUser(user);
        return this.cvDraftRepository.save(draft);
    }

    /**
     * Lấy danh sách CV drafts của user đang đăng nhập (có phân trang).
     */
    public ResultPaginationDTO getCvDraftsByUser(String userEmail, Pageable pageable) throws IdInvalidException {
        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("Không tìm thấy user với email: " + userEmail);
        }

        Page<CvDraft> pageData = this.cvDraftRepository.findByUserId(user.getId(), pageable);

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(pageData.getTotalPages());
        meta.setTotal(pageData.getTotalElements());
        result.setMeta(meta);
        result.setResult(pageData.getContent());
        return result;
    }

    /**
     * Lấy một CV draft theo ID, chỉ trả về nếu thuộc về user hiện tại.
     */
    public CvDraft getCvDraftById(Long id, String userEmail) throws IdInvalidException {
        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("Không tìm thấy user với email: " + userEmail);
        }
        Optional<CvDraft> draftOpt = this.cvDraftRepository.findByIdAndUserId(id, user.getId());
        if (!draftOpt.isPresent()) {
            throw new IdInvalidException("Không tìm thấy CV với id = " + id + " hoặc bạn không có quyền truy cập");
        }
        return draftOpt.get();
    }

    /**
     * Xóa CV draft, chỉ xóa được CV của chính mình.
     */
    public void deleteCvDraft(Long id, String userEmail) throws IdInvalidException {
        User user = this.userService.handleGetUserByUsername(userEmail);
        if (user == null) {
            throw new IdInvalidException("Không tìm thấy user với email: " + userEmail);
        }
        Optional<CvDraft> draftOpt = this.cvDraftRepository.findByIdAndUserId(id, user.getId());
        if (!draftOpt.isPresent()) {
            throw new IdInvalidException("Không tìm thấy CV với id = " + id + " hoặc bạn không có quyền xóa");
        }
        this.cvDraftRepository.deleteById(id);
    }

    // ===== Conversion Helpers =====

    public ResCreateCvDraftDTO convertToResCreateCvDraftDTO(CvDraft draft) {
        ResCreateCvDraftDTO dto = new ResCreateCvDraftDTO();
        dto.setId(draft.getId());
        dto.setTitle(draft.getTitle());
        dto.setCreatedAt(draft.getCreatedAt());
        dto.setCreatedBy(draft.getCreatedBy());
        return dto;
    }

    public ResFetchCvDraftDTO convertToResFetchCvDraftDTO(CvDraft draft) {
        ResFetchCvDraftDTO dto = new ResFetchCvDraftDTO();
        dto.setId(draft.getId());
        dto.setTitle(draft.getTitle());
        dto.setCvJsonData(draft.getCvJsonData());
        dto.setTemplateId(draft.getTemplateId());
        dto.setAvatarUrl(draft.getAvatarUrl());
        dto.setCreatedAt(draft.getCreatedAt());
        dto.setUpdatedAt(draft.getUpdatedAt());
        dto.setCreatedBy(draft.getCreatedBy());
        return dto;
    }
}
