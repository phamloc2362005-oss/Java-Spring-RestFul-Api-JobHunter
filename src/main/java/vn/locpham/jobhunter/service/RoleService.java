package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Permission;
import vn.locpham.jobhunter.domain.Role;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.repository.PermissionRepository;
import vn.locpham.jobhunter.repository.RoleRepository;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public boolean isNameExist(String name) {
        return this.roleRepository.existsByName(name);
    }

    public Role fetchRoleById(long id) {
        Optional<Role> roleOptional = this.roleRepository.findById(id);
        return roleOptional.isPresent() ? roleOptional.get() : null;
    }

    public Role createNewRole(Role reqRole) {
        // check Permission
        if (reqRole.getPermissions() != null) {
            List<Long> reqPermissions = reqRole.getPermissions().stream()
                    .map(x -> x.getId()).collect(Collectors.toList());
            List<Permission> dbPermissions = this.permissionRepository.findByIdIn(reqPermissions);
            reqRole.setPermissions(dbPermissions);
        }
        return this.roleRepository.save(reqRole);
    }

    public Role updateRole(Role reqRole) {
        Role roleDB = this.fetchRoleById(reqRole.getId());
        if (reqRole.getPermissions() != null) {
            List<Long> reqPermissions = reqRole.getPermissions().stream()
                    .map(x -> x.getId()).collect(Collectors.toList());
            List<Permission> dbPermissions = this.permissionRepository.findByIdIn(reqPermissions);
            reqRole.setPermissions(dbPermissions);
        }
        roleDB.setName(reqRole.getName());
        roleDB.setDescription(reqRole.getDescription());
        roleDB.setActive(reqRole.isActive());
        roleDB.setPermissions(reqRole.getPermissions());
        return this.roleRepository.save(roleDB);
    }

    public void deleteRole(long id) {
        this.roleRepository.deleteById(id);
    }

    public ResultPaginationDTO fetchAllRoles(Specification<Role> spec, Pageable pageable) {
        Page<Role> pageRoles = this.roleRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageRoles.getTotalPages());
        mt.setTotal(pageRoles.getTotalElements());
        rs.setResult(pageRoles.getContent());
        rs.setMeta(mt);
        return rs;
    }

}
