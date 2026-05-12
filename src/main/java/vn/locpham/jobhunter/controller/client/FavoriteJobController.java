package vn.locpham.jobhunter.controller.client;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.locpham.jobhunter.domain.Job;
import vn.locpham.jobhunter.service.FavoriteJobService;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class FavoriteJobController {
    private final FavoriteJobService favoriteJobService;

    public FavoriteJobController(FavoriteJobService favoriteJobService) {
        this.favoriteJobService = favoriteJobService;
    }

    @GetMapping("/favorites")
    @ApiMessage("Fetch favorite jobs")
    public ResponseEntity<List<Job>> fetchFavorites() throws IdInvalidException {
        String email = requireEmail();
        return ResponseEntity.status(HttpStatus.OK).body(this.favoriteJobService.getFavoriteJobs(email));
    }

    @GetMapping("/favorites/{jobId}")
    @ApiMessage("Check favorite job")
    public ResponseEntity<Boolean> checkFavorite(@PathVariable("jobId") Long jobId) throws IdInvalidException {
        String email = requireEmail();
        return ResponseEntity.status(HttpStatus.OK).body(this.favoriteJobService.isFavorite(email, jobId));
    }

    @PostMapping("/favorites/{jobId}")
    @ApiMessage("Add favorite job")
    public ResponseEntity<Boolean> addFavorite(@PathVariable("jobId") Long jobId) throws IdInvalidException {
        String email = requireEmail();
        return ResponseEntity.status(HttpStatus.OK).body(this.favoriteJobService.addFavorite(email, jobId));
    }

    @DeleteMapping("/favorites/{jobId}")
    @ApiMessage("Remove favorite job")
    public ResponseEntity<Boolean> removeFavorite(@PathVariable("jobId") Long jobId) throws IdInvalidException {
        String email = requireEmail();
        return ResponseEntity.status(HttpStatus.OK).body(this.favoriteJobService.removeFavorite(email, jobId));
    }

    private String requireEmail() throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        if (email == null || email.isEmpty()) {
            throw new IdInvalidException("Bạn chưa đăng nhập");
        }
        return email;
    }
}
