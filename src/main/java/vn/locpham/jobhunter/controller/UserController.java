package vn.locpham.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.locpham.jobhunter.util.SecurityUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.RestResponse;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;

import vn.locpham.jobhunter.domain.reponse.user.ResCreateUserDTO;
import vn.locpham.jobhunter.domain.reponse.user.ResUpdateUserDTO;
import vn.locpham.jobhunter.domain.reponse.user.ResUserDTO;
import vn.locpham.jobhunter.domain.request.UpdateUserProfileForRecommendationDTO;
import vn.locpham.jobhunter.service.RecommendationService;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RecommendationService recommendationService;

    public UserController(UserService userService, PasswordEncoder passwordEncoder,
            RecommendationService recommendationService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.recommendationService = recommendationService;
    }

    @PostMapping("/users")
    @ApiMessage("Create a new user")
    public ResponseEntity<ResCreateUserDTO> createNewUser(@Valid @RequestBody User postmanUser)
            throws IdInvalidException {
        boolean isEmailExist = this.userService.isExistEmail(postmanUser.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException("Email " + postmanUser.getEmail() + "đã tồn tại, vui lòng sử dụng email khác");
        }
        String hashPassword = this.passwordEncoder.encode(postmanUser.getPassword());
        postmanUser.setPassword(hashPassword);
        User user = this.userService.handleCreateUser(postmanUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(user));

    }

    @DeleteMapping("/users/{id}")
    @ApiMessage("Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) throws IdInvalidException {
        User currentUser = this.userService.fetchUserById(id);
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }
        this.userService.handleDeleteUser(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/users/{id}")
    @ApiMessage("Fetch user by id")
    public ResponseEntity<ResUserDTO> fetchUserById(@PathVariable("id") Long id) throws IdInvalidException {
        User currentUser = this.userService.fetchUserById(id);
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }
        return ResponseEntity.status(HttpStatus.OK).body(this.userService.convertToResUserDTO(currentUser));

    }

    @GetMapping("/users")
    @ApiMessage("Fetch all users")
    public ResponseEntity<ResultPaginationDTO> getAllUser(@Filter Specification<User> spec, Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(this.userService.fetchAllUser(spec, pageable));

    }

    @PutMapping("/users")
    @ApiMessage("Update a user")
    public ResponseEntity<ResUpdateUserDTO> updateUser(@RequestBody User user) throws IdInvalidException {
        User currentUser = this.userService.handleUpdateUser(user);
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + user.getId() + " không tồn tại");
        }
        // Clear recommendation cache
        this.recommendationService.clearCache(currentUser.getId());
        return ResponseEntity.ok(this.userService.convertToResUpdateUserDTO(currentUser));
    }

    /**
     * API lấy user profile hiện tại cho AI recommendation
     * GET /api/v1/users/profile/recommendation
     */
    @GetMapping("/users/profile/recommendation")
    @ApiMessage("Get user profile for AI recommendations")
    public ResponseEntity<UpdateUserProfileForRecommendationDTO> getUserProfileForRecommendation() throws IdInvalidException {

        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) {
            throw new IdInvalidException("Unauthorized - Please login");
        }

        User currentUser = this.userService.handleGetUserByUsername(email);
        if (currentUser == null) {
            throw new IdInvalidException("Unauthorized - Please login");
        }

        UpdateUserProfileForRecommendationDTO profile = this.userService
                .getUserProfileForRecommendation(currentUser.getId());

        return ResponseEntity.ok(profile);
    }

    /**
     * API cập nhật user profile cho AI recommendation
     * PUT /api/v1/users/profile/recommendation
     * Cập nhật skills, level, expertise của user
     */
    @PutMapping("/users/profile/recommendation")
    @ApiMessage("Update user profile for AI recommendations")
    public ResponseEntity<ResUpdateUserDTO> updateUserProfileForRecommendation(
            @RequestBody UpdateUserProfileForRecommendationDTO dto) throws IdInvalidException {

        String email = SecurityUtils.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) {
            throw new IdInvalidException("Unauthorized - Please login");
        }

        User currentUser = this.userService.handleGetUserByUsername(email);
        if (currentUser == null) {
            throw new IdInvalidException("Unauthorized - Please login");
        }

        User updatedUser = this.userService.updateUserProfileForRecommendation(currentUser.getId(), dto);
        if (updatedUser == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        // Clear recommendation cache so AI recalculates with new profile
        this.recommendationService.clearCache(updatedUser.getId());

        return ResponseEntity.ok(this.userService.convertToResUpdateUserDTO(updatedUser));
    }

    // Using email-based lookup instead of extracting userId from token
}
