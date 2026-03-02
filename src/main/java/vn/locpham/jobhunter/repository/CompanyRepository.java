package vn.locpham.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.locpham.jobhunter.domain.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

}
