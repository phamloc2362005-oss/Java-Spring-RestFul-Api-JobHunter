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
import vn.locpham.jobhunter.domain.Role;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.service.RoleService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class RoleController {
    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping("/roles")
    @ApiMessage("create a new role")
    public ResponseEntity<Role> createNewRole(@Valid @RequestBody Role reqRole) throws IdInvalidException {
        // check name
        if (this.roleService.isNameExist(reqRole.getName())) {
            throw new IdInvalidException("Role da ton tai");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(this.roleService.createNewRole(reqRole));
    }

    @PutMapping("/roles")
    @ApiMessage("update a role")
    public ResponseEntity<Role> updateRole(@Valid @RequestBody Role reqRole) throws IdInvalidException {
        // check id
        if (this.roleService.fetchRoleById(reqRole.getId()) == null) {
            throw new IdInvalidException("Role voi id = " + reqRole.getId() + "khong ton tai");
        }
        // check name
        // if (this.roleService.isNameExist(reqRole.getName())) {
        // throw new IdInvalidException("Role da ton tai");
        // }
        return ResponseEntity.status(HttpStatus.OK).body(this.roleService.updateRole(reqRole));
    }

    @DeleteMapping("roles/{id}")
    @ApiMessage("delete a role")
    public ResponseEntity<Void> deleteRole(@PathVariable("id") long id) throws IdInvalidException {
        // check id
        if (this.roleService.fetchRoleById(id) == null) {
            throw new IdInvalidException("Role voi id = " + id + "khong ton tai");
        }
        this.roleService.deleteRole(id);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/roles")
    @ApiMessage("Fetch all roles")
    public ResponseEntity<ResultPaginationDTO> getAllRoles(@Filter Specification<Role> spec,
            Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(this.roleService.fetchAllRoles(spec, pageable));

    }
}
