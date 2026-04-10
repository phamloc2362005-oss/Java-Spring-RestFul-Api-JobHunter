package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Expertise;
import vn.locpham.jobhunter.domain.ExpertiseCategory;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.expertise.ResCreateExpertiseDTO;
import vn.locpham.jobhunter.domain.reponse.expertise.ResUpdateExpertiseDTO;
import vn.locpham.jobhunter.repository.ExpertiseRepository;

@Service
public class ExpertiseService {
    private final ExpertiseRepository expertiseRepository;
    private final ExpertiseCategoryService expertiseCategoryService;

    public ExpertiseService(ExpertiseRepository expertiseRepository,
            ExpertiseCategoryService expertiseCategoryService) {
        this.expertiseRepository = expertiseRepository;
        this.expertiseCategoryService = expertiseCategoryService;
    }

    public boolean isNameExist(String name) {
        return this.expertiseRepository.existsByName(name);
    }

    public Expertise fetchExpertiseById(long id) {
        return this.expertiseRepository.findById(id).orElse(null);
    }

    public ResCreateExpertiseDTO handleCreateNewExpertise(Expertise expertise) {
        if (expertise.getExpertiseCategory() != null) {
            ExpertiseCategory expertiseCategory = this.expertiseCategoryService
                    .fetchExpertiseCategoryById(expertise.getExpertiseCategory().getId());
            if (expertiseCategory != null) {
                expertise.setExpertiseCategory(expertiseCategory);
            }
        }
        Expertise newExpertise = this.expertiseRepository.save(expertise);
        ResCreateExpertiseDTO rs = new ResCreateExpertiseDTO();
        rs.setId(newExpertise.getId());
        rs.setName(newExpertise.getName());
        rs.setExpertiseCategoryName(newExpertise.getExpertiseCategory().getName());
        rs.setCreatedAt(newExpertise.getCreatedAt());
        rs.setCreatedBy(newExpertise.getCreatedBy());
        return rs;
    }

    public ResUpdateExpertiseDTO handleUpdateExpertise(Expertise reqExpertise, Expertise expertiseInDB) {
        if (reqExpertise.getExpertiseCategory() != null) {
            ExpertiseCategory expertiseCategory = this.expertiseCategoryService
                    .fetchExpertiseCategoryById(reqExpertise.getExpertiseCategory().getId());
            if (expertiseCategory != null) {
                expertiseInDB.setExpertiseCategory(expertiseCategory);
            }
        }
        expertiseInDB.setName(reqExpertise.getName());
        Expertise updateExpertise = this.expertiseRepository.save(expertiseInDB);
        ResUpdateExpertiseDTO rs = new ResUpdateExpertiseDTO();
        rs.setId(updateExpertise.getId());
        rs.setName(updateExpertise.getName());
        rs.setExpertiseCategoryName(updateExpertise.getExpertiseCategory().getName());
        rs.setUpdatedAt(updateExpertise.getUpdatedAt());
        rs.setUpdatedBy(updateExpertise.getUpdatedBy());
        return rs;

    }

    public ResultPaginationDTO fetchAllExpertise(Specification<Expertise> spec, Pageable pageable) {
        Page<Expertise> pageExpertise = this.expertiseRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageExpertise.getTotalPages());
        mt.setTotal(pageExpertise.getTotalElements());

        List<Expertise> listExpertise = pageExpertise.getContent();
        rs.setMeta(mt);
        rs.setResult(listExpertise);
        return rs;
    }

    public void handleDeleteExpertise(long id) {
        Optional<Expertise> expertiseOptional = this.expertiseRepository.findById(id);
        if (expertiseOptional.isEmpty())
            return;
        Expertise currentExpertise = expertiseOptional.get();
        currentExpertise.getJobs().forEach(job -> job.getExpertises().remove(currentExpertise));
        this.expertiseRepository.delete(currentExpertise);
    }
}
