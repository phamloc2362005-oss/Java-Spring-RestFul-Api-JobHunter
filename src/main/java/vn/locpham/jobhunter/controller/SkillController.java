package vn.locpham.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.service.SkillService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.turkraft.springfilter.boot.Filter;

import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;

@RestController
@RequestMapping("/api/v1")
public class SkillController {
    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping("/skills")
    @ApiMessage("Create a new skill")
    public ResponseEntity<Skill> createNewSkill(@Valid @RequestBody Skill postmanSkill) throws IdInvalidException {
        boolean isNameExist = this.skillService.isNameExist(postmanSkill.getName());
        if (isNameExist) {
            throw new IdInvalidException("Skill này đã tồn tại, vui lòng nhập skill khác");
        }
        Skill skill = this.skillService.handleCreateNewSkill(postmanSkill);
        return ResponseEntity.status(HttpStatus.CREATED).body(skill);
    }

    @PutMapping("/skills")
    @ApiMessage("Update a skill")
    public ResponseEntity<Skill> updateSkill(@Valid @RequestBody Skill reqSkill) throws IdInvalidException {
        boolean isNameExist = this.skillService.isNameExist(reqSkill.getName());
        if (isNameExist) {
            throw new IdInvalidException("Skill này đã tồn tại, vui lòng nhập skill khác");
        }
        Skill skill = this.skillService.handleUpdateSkill(reqSkill);
        return ResponseEntity.status(HttpStatus.OK).body(skill);
    }

    @GetMapping("/skills/{id}")
    @ApiMessage("Fetch a skill")
    public ResponseEntity<Skill> getSkillById(@PathVariable("id") Long id) throws IdInvalidException {
        Skill currentSkill = this.skillService.fetchSkillById(id);
        if (currentSkill == null) {
            throw new IdInvalidException("Skill với id = " + id + " không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(currentSkill);
    }

    @GetMapping("/skills")
    @ApiMessage("Fetch all skills")
    public ResponseEntity<ResultPaginationDTO> getAllSkill(@Filter Specification<Skill> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(this.skillService.fetchAllSkill(spec, pageable));

    }

    @DeleteMapping("/skills/{id}")
    @ApiMessage("Delete a skill")
    public ResponseEntity<Void> DeleteSkillById(@PathVariable("id") Long id) throws IdInvalidException {
        Skill currentSkill = this.skillService.fetchSkillById(id);
        if (currentSkill == null) {
            throw new IdInvalidException("Skill với id = " + id + " không tồn tại");
        }
        this.skillService.handleDeleteSkill(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

}
