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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.Permission;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.service.PermissionService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("api/v1")
public class PermissionController {
    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping("/permissions")
    @ApiMessage("Create a permission")
    public ResponseEntity<Permission> createNewPermission(@Valid @RequestBody Permission reqPermission)
            throws IdInvalidException {
        if (this.permissionService.isPermissionExist(reqPermission)) {
            throw new IdInvalidException("Permission da ton tai");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.permissionService.createNewPermission(reqPermission));
    }

    @PutMapping("/permissions")
    @ApiMessage("update a permission")
    public ResponseEntity<Permission> updatePermission(@Valid @RequestBody Permission reqPermission)
            throws IdInvalidException {
        if (this.permissionService.fetchById(reqPermission.getId()) == null) {
            throw new IdInvalidException("Permission voi id = " + reqPermission.getId() + "khong ton tai");
        }
        if (this.permissionService.isPermissionExist(reqPermission)) {
            throw new IdInvalidException("Permission da ton tai");
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(this.permissionService.updatePermission(reqPermission));
    }

    @GetMapping("/permissions")
    @ApiMessage("Fetch all permissions")
    public ResponseEntity<ResultPaginationDTO> getAllPermissions(@Filter Specification<Permission> spec,
            Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(this.permissionService.fetchAllPermissions(spec, pageable));

    }

    @DeleteMapping("permissions/{id}")
    @ApiMessage("delete a role")
    public ResponseEntity<Void> deletePermission(@PathVariable("id") long id) throws IdInvalidException {
        // check id
        if (this.permissionService.fetchById(id) == null) {
            throw new IdInvalidException("Permission voi id = " + id + "khong ton tai");
        }
        this.permissionService.deletePermission(id);
        return ResponseEntity.ok(null);
    }
}
