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
import vn.locpham.jobhunter.domain.ExpertiseCategory;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.service.ExpertiseCategoryService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class ExpertiseCategoryController {
    private final ExpertiseCategoryService expertiseCategoryService;

    public ExpertiseCategoryController(ExpertiseCategoryService expertiseCategoryService) {
        this.expertiseCategoryService = expertiseCategoryService;
    }

    @PostMapping("/expertise-category")
    @ApiMessage("Tạo một category mới")
    public ResponseEntity<ExpertiseCategory> createNewExpertiseCategory(
            @Valid @RequestBody ExpertiseCategory reqExpertiseCategory) throws IdInvalidException {
        boolean isNameExist = this.expertiseCategoryService.isNameExist(reqExpertiseCategory.getName());
        if (isNameExist) {
            throw new IdInvalidException("Tên category này đã tồn tại, vui lòng nhập tên khác");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.expertiseCategoryService
                .handleCreateNewExpertiseCategory(reqExpertiseCategory));
    }

    @PutMapping("/expertise-category")
    @ApiMessage("Cập nhật một category")
    public ResponseEntity<ExpertiseCategory> updateExpertiseCategory(
            @Valid @RequestBody ExpertiseCategory reqExpertiseCategory) throws IdInvalidException {
        ExpertiseCategory expertiseCategoryInDB = this.expertiseCategoryService
                .fetchExpertiseCategoryById(reqExpertiseCategory.getId());
        if (expertiseCategoryInDB == null) {
            throw new IdInvalidException("Không tìm thấy category với id = " + reqExpertiseCategory.getId());
        }
        boolean isNameExist = this.expertiseCategoryService.isNameExist(reqExpertiseCategory.getName());
        if (isNameExist) {
            throw new IdInvalidException("Tên category này đã tồn tại, vui lòng nhập tên khác");
        }
        return ResponseEntity.status(HttpStatus.OK).body(this.expertiseCategoryService
                .handleUpdateExpertiseCategory(reqExpertiseCategory, expertiseCategoryInDB));
    }

    @DeleteMapping("/expertise-category/{id}")
    @ApiMessage("Xóa một category")
    public ResponseEntity<Void> deleteExpertiseCategory(@PathVariable("id") long id) throws IdInvalidException {
        ExpertiseCategory expertiseCategoryInDB = this.expertiseCategoryService.fetchExpertiseCategoryById(id);
        if (expertiseCategoryInDB == null) {
            throw new IdInvalidException("Không tìm thấy category với id = " + id);
        }
        this.expertiseCategoryService.handleDeleteExpertiseCategory(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/expertise-category/{id}")
    @ApiMessage("Lấy thông tin category theo id")
    public ResponseEntity<ExpertiseCategory> getExpertiseCategoryById(@PathVariable("id") long id)
            throws IdInvalidException {
        ExpertiseCategory expertiseCategoryInDB = this.expertiseCategoryService.fetchExpertiseCategoryById(id);
        if (expertiseCategoryInDB == null) {
            throw new IdInvalidException("Không tìm thấy category với id = " + id);
        }
        return ResponseEntity.status(HttpStatus.OK).body(expertiseCategoryInDB);
    }

    @GetMapping("/expertise-category")
    @ApiMessage("Lấy tất cả category")
    public ResultPaginationDTO getAllExpertiseCategory(@Filter Specification<ExpertiseCategory> spec,
            Pageable pageable) {
        return this.expertiseCategoryService.fetchAllExpertiseCategory(spec, pageable);
    }
}
