package vn.locpham.jobhunter.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Meta;
import vn.locpham.jobhunter.domain.ResultPaginationDTO;
import vn.locpham.jobhunter.repository.CompanyRepository;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company handleCreateNewCompany(Company company) {
        return this.companyRepository.save(company);
    }

    public ResultPaginationDTO fetchAllCompanies(Specification<Company> spec, Pageable pageable) {
        Page<Company> pageCompany = this.companyRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        Meta mt = new Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageCompany.getTotalPages());
        mt.setTotal(pageCompany.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageCompany.getContent());
        return rs;
    }

    public Company handleUpdateCompany(Company reqCompany) {
        Optional<Company> optionalCompany = this.companyRepository.findById(reqCompany.getId());
        if (optionalCompany.isPresent()) {
            Company updateCompany = optionalCompany.get();
            updateCompany.setName(reqCompany.getName());
            updateCompany.setLogo(reqCompany.getLogo());
            updateCompany.setDescription(reqCompany.getDescription());
            updateCompany.setCreatedAt(reqCompany.getCreatedAt());
            updateCompany.setCreatedBy(reqCompany.getCreatedBy());
            return this.companyRepository.save(updateCompany);
        }
        return null;
    }

    public void handleDeleteCompany(long id) {
        this.companyRepository.deleteById(id);
    }
}
