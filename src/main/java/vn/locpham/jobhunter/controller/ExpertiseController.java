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
import vn.locpham.jobhunter.domain.Expertise;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.expertise.ResCreateExpertiseDTO;
import vn.locpham.jobhunter.domain.reponse.expertise.ResUpdateExpertiseDTO;
import vn.locpham.jobhunter.service.ExpertiseService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class ExpertiseController {
    private final ExpertiseService expertiseService;

    public ExpertiseController(ExpertiseService expertiseService) {
        this.expertiseService = expertiseService;
    }

    @PostMapping("/expertise")
    @ApiMessage("Create a new expertise")
    public ResponseEntity<ResCreateExpertiseDTO> createNewExpertise(@Valid @RequestBody Expertise postmanExpertise)
            throws IdInvalidException {
        boolean isNameExist = this.expertiseService.isNameExist(postmanExpertise.getName());
        if (isNameExist) {
            throw new IdInvalidException("Expertise này đã tồn tại, vui lòng nhập expertise khác");
        }
        ResCreateExpertiseDTO expertise = this.expertiseService.handleCreateNewExpertise(postmanExpertise);
        return ResponseEntity.status(HttpStatus.CREATED).body(expertise);
    }

    @PutMapping("/expertise")
    @ApiMessage("Update a expertise")
    public ResponseEntity<ResUpdateExpertiseDTO> updateExpertise(@Valid @RequestBody Expertise reqExpertise)
            throws IdInvalidException {
        Expertise expertiseInDB = this.expertiseService.fetchExpertiseById(reqExpertise.getId());
        if (expertiseInDB == null) {
            throw new IdInvalidException("Expertise với id = " + reqExpertise.getId() + " không tồn tại");
        }
        boolean isNameExist = this.expertiseService.isNameExist(reqExpertise.getName());
        if (isNameExist
                && reqExpertise.getExpertiseCategory().getId() == expertiseInDB.getExpertiseCategory().getId()) {
            throw new IdInvalidException("Expertise này đã tồn tại, vui lòng nhập expertise khác");
        }
        ResUpdateExpertiseDTO expertise = this.expertiseService.handleUpdateExpertise(reqExpertise, expertiseInDB);
        return ResponseEntity.status(HttpStatus.OK).body(expertise);
    }

    @GetMapping("/expertise/{id}")
    @ApiMessage("Fetch a expertise")
    public ResponseEntity<Expertise> getExpertiseById(@PathVariable("id") Long id) throws IdInvalidException {
        Expertise expertise = this.expertiseService.fetchExpertiseById(id);
        if (expertise == null) {
            throw new IdInvalidException("Expertise với id = " + id + " không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(expertise);
    }

    @GetMapping("/expertise")
    @ApiMessage("Fetch all expertise")
    public ResponseEntity<ResultPaginationDTO> getAllExpertise(@Filter Specification<Expertise> spec,
            Pageable pageable) {
        ResultPaginationDTO rs = this.expertiseService.fetchAllExpertise(spec, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(rs);
    }

    @DeleteMapping("/expertise/{id}")
    @ApiMessage("Delete a expertise")
    public ResponseEntity<Void> DeleteExpertiseById(@PathVariable("id") Long id) throws IdInvalidException {
        Expertise expertise = this.expertiseService.fetchExpertiseById(id);
        if (expertise == null) {
            throw new IdInvalidException("Expertise với id = " + id + " không tồn tại");
        }
        this.expertiseService.handleDeleteExpertise(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
