package vn.locpham.jobhunter.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Expertise;
import vn.locpham.jobhunter.domain.ExpertiseCategory;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.repository.ExpertiseCategoryRepository;
import vn.locpham.jobhunter.repository.ExpertiseRepository;

@Service
public class ExpertiseCategoryService {
    private final ExpertiseCategoryRepository expertiseCategoryRepository;
    private final ExpertiseRepository expertiseRepository;

    public ExpertiseCategoryService(ExpertiseCategoryRepository expertiseCategoryRepository,
            ExpertiseRepository expertiseRepository) {
        this.expertiseCategoryRepository = expertiseCategoryRepository;
        this.expertiseRepository = expertiseRepository;
    }

    public boolean isNameExist(String name) {
        return this.expertiseCategoryRepository.existsByName(name);
    }

    public ExpertiseCategory handleCreateNewExpertiseCategory(ExpertiseCategory expertiseCategory) {
        return this.expertiseCategoryRepository.save(expertiseCategory);
    }

    public ExpertiseCategory fetchExpertiseCategoryById(long id) {
        return this.expertiseCategoryRepository.findById(id).orElse(null);
    }

    public ExpertiseCategory handleUpdateExpertiseCategory(ExpertiseCategory reqExpertiseCategory,
            ExpertiseCategory expertiseCategoryInDB) {
        expertiseCategoryInDB.setName(reqExpertiseCategory.getName());
        return this.expertiseCategoryRepository.save(expertiseCategoryInDB);
    }

    public void handleDeleteExpertiseCategory(long id) {
        ExpertiseCategory expertiseCategory = this.fetchExpertiseCategoryById(id);
        if (expertiseCategory != null) {
            List<Expertise> expertises = expertiseCategory.getExpertises();
            expertises.forEach(expertise -> expertise.setExpertiseCategory(null));
            this.expertiseCategoryRepository.delete(expertiseCategory);
        }
    }

    public ResultPaginationDTO fetchAllExpertiseCategory(Specification<ExpertiseCategory> spec, Pageable pageable) {
        Page<ExpertiseCategory> pageExpertiseCategory = this.expertiseCategoryRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageExpertiseCategory.getTotalPages());
        mt.setTotal(pageExpertiseCategory.getTotalElements());

        List<ExpertiseCategory> listExpertiseCategory = pageExpertiseCategory.getContent();
        rs.setMeta(mt);
        rs.setResult(listExpertiseCategory);
        return rs;
    }
}