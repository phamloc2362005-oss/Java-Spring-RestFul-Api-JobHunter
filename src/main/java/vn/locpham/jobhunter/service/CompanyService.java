package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
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

    public List<Company> fetchAllCompanies() {
        return this.companyRepository.findAll();
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
