package vn.locpham.jobhunter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import vn.locpham.jobhunter.domain.Permission;

public interface PermissionRepository
        extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {
    boolean existsByModuleAndApiPathAndMethod(String module, String apiPath, String method);

    Page<Permission> findAll(Specification<Permission> spec, Pageable pageable);
}
