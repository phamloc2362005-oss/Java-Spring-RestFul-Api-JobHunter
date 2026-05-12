package vn.locpham.jobhunter.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.locpham.jobhunter.domain.Company;
import vn.locpham.jobhunter.domain.Expertise;
import vn.locpham.jobhunter.domain.Role;
import vn.locpham.jobhunter.domain.Skill;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResultPaginationDTO;
import vn.locpham.jobhunter.domain.reponse.user.ResCreateUserDTO;
import vn.locpham.jobhunter.domain.reponse.user.ResUpdateUserDTO;
import vn.locpham.jobhunter.domain.reponse.user.ResUserDTO;
import vn.locpham.jobhunter.domain.request.UpdateUserProfileForRecommendationDTO;
import vn.locpham.jobhunter.repository.ExpertiseRepository;
import vn.locpham.jobhunter.repository.SkillRepository;
import vn.locpham.jobhunter.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final RoleService roleService;
    private final ExpertiseRepository expertiseRepository;
    private final SkillRepository skillRepository;

    public UserService(UserRepository userRepository, CompanyService companyService, RoleService roleService,
            ExpertiseRepository expertiseRepository, SkillRepository skillRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.roleService = roleService;
        this.expertiseRepository = expertiseRepository;
        this.skillRepository = skillRepository;
    }

    public User handleCreateUser(User user) {
        // nếu user có company thì lấy company thật từ DB gắn vào
        if (user.getCompany() != null) {
            Optional<Company> comOptional = this.companyService.findById(user.getCompany().getId());
            user.setCompany(comOptional.isPresent() ? comOptional.get() : null);
        }
        // nếu user có role thì lấy role thật từ DB gắn vào
        if (user.getRole() != null) {
            Role role = this.roleService.fetchRoleById(user.getRole().getId());
            user.setRole(role);
        }
        // lưu user vào DB
        return this.userRepository.save(user);
    }

    public void handleDeleteUser(long id) {
        this.userRepository.deleteById(id);
    }

    public User fetchUserById(long id) {
        // tìm user theo id
        Optional<User> user = this.userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        }
        return null;
    }

    public ResultPaginationDTO fetchAllUser(Specification<User> spec, Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        List<ResUserDTO> listUser = pageUser.getContent().stream()
                .map(item -> this.convertToResUserDTO(item))
                .collect(Collectors.toList());

        rs.setMeta(mt);
        rs.setResult(listUser);
        return rs;
    }

    public User handleUpdateUser(User reqUser) {
        User currentUser = this.fetchUserById(reqUser.getId());
        // tìm user cũ trong DB
        if (currentUser != null) {
            // update các field cơ bản
            currentUser.setAddress(reqUser.getAddress());
            currentUser.setName(reqUser.getName());
            currentUser.setAge(reqUser.getAge());
            currentUser.setGender(reqUser.getGender());
            // update company nếu có
            if (reqUser.getCompany() != null) {
                Optional<Company> comOptional = this.companyService.findById(reqUser.getCompany().getId());
                currentUser.setCompany(comOptional.isPresent() ? comOptional.get() : null);
            }

            // update role nếu có
            if (reqUser.getRole() != null) {
                Role role = this.roleService.fetchRoleById(reqUser.getRole().getId());
                currentUser.setRole(role);
            }
            // lưu lại DB
            this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public boolean isExistEmail(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        ResCreateUserDTO resCreateUserDTO = new ResCreateUserDTO();
        ResCreateUserDTO.CompanyUser company = new ResCreateUserDTO.CompanyUser();
        if (user.getCompany() != null) {
            company.setId(user.getCompany().getId());
            company.setName(user.getCompany().getName());
            resCreateUserDTO.setCompany(company);
        }

        resCreateUserDTO.setId(user.getId());
        resCreateUserDTO.setName(user.getName());
        resCreateUserDTO.setEmail(user.getEmail());
        resCreateUserDTO.setGender(user.getGender());
        resCreateUserDTO.setAddress(user.getAddress());
        resCreateUserDTO.setAge(user.getAge());
        resCreateUserDTO.setCreatedAt(user.getCreatedAt());
        resCreateUserDTO.setCreatedBy(user.getCreatedBy());
        return resCreateUserDTO;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO resUserDTO = new ResUserDTO();
        ResUserDTO.CompanyUser company = new ResUserDTO.CompanyUser();
        ResUserDTO.RoleUser role = new ResUserDTO.RoleUser();
        if (user.getCompany() != null) {
            company.setId(user.getCompany().getId());
            company.setName(user.getCompany().getName());
            resUserDTO.setCompany(company);
        }
        if (user.getRole() != null) {
            role.setId(user.getRole().getId());
            role.setName(user.getRole().getName());
            resUserDTO.setRole(role);
        }
        resUserDTO.setId(user.getId());
        resUserDTO.setName(user.getName());
        resUserDTO.setEmail(user.getEmail());
        resUserDTO.setGender(user.getGender());
        resUserDTO.setAddress(user.getAddress());
        resUserDTO.setAge(user.getAge());
        resUserDTO.setCreatedAt(user.getCreatedAt());
        resUserDTO.setUpdatedAt(user.getUpdatedAt());
        return resUserDTO;
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        ResUpdateUserDTO resUserUpdateDTO = new ResUpdateUserDTO();
        ResUpdateUserDTO.CompanyUser company = new ResUpdateUserDTO.CompanyUser();
        if (user.getCompany() != null) {
            company.setId(user.getCompany().getId());
            company.setName(user.getCompany().getName());
            resUserUpdateDTO.setCompany(company);
        }
        resUserUpdateDTO.setId(user.getId());
        resUserUpdateDTO.setName(user.getName());
        resUserUpdateDTO.setGender(user.getGender());
        resUserUpdateDTO.setAddress(user.getAddress());
        resUserUpdateDTO.setAge(user.getAge());
        resUserUpdateDTO.setUpdatedAt(user.getUpdatedAt());
        resUserUpdateDTO.setUpdatedBy(user.getUpdatedBy());
        return resUserUpdateDTO;
    }

    // lưu refresh token vào DB
    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    // check refresh token hợp lệ
    public User getUserByRefreshTokenAndEmail(String token, String email) {
        // dùng khi refresh token:
        // check token này có đúng là token của user trong DB không
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    // logout
    public void handleDeleteRefreshTokenByEmail(String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            // logout -> xóa refresh token trong DB
            currentUser.setRefreshToken(null);
            this.userRepository.save(currentUser);
        }
    }

    /**
     * Cập nhật user profile cho AI recommendation system
     * Service xử lý logic: fetch expertise, skills từ DB, cập nhật user
     */
    public User updateUserProfileForRecommendation(Long userId, UpdateUserProfileForRecommendationDTO dto) {
        User currentUser = this.fetchUserById(userId);
        if (currentUser == null) {
            return null;
        }

        // Cập nhật level nếu có
        if (dto.getLevel() != null) {
            currentUser.setLevel(dto.getLevel());
        }

        // Cập nhật expertise nếu có
        if (dto.getExpertiseId() != null && dto.getExpertiseId() > 0) {
            Optional<Expertise> expertiseOptional = this.expertiseRepository.findById(dto.getExpertiseId());
            if (expertiseOptional.isPresent()) {
                currentUser.setExpertise(expertiseOptional.get());
            }
        }

        // Cập nhật skills nếu có (có thể là list rỗng)
        if (dto.getSkillIds() != null && !dto.getSkillIds().isEmpty()) {
            List<Skill> skills = this.skillRepository.findAllById(dto.getSkillIds());
            currentUser.setSkills(skills);
        }

        // Lưu vào DB
        return this.userRepository.save(currentUser);
    }

    public UpdateUserProfileForRecommendationDTO getUserProfileForRecommendation(Long userId) {
        User currentUser = this.fetchUserById(userId);
        if (currentUser == null) {
            return null;
        }

        List<Long> skillIds = currentUser.getSkills() == null
                ? List.of()
                : currentUser.getSkills().stream().map(Skill::getId).collect(Collectors.toList());

        List<UpdateUserProfileForRecommendationDTO.SkillInfo> skillDetails = currentUser.getSkills() == null
                ? List.of()
                : currentUser.getSkills().stream()
                        .map(s -> new UpdateUserProfileForRecommendationDTO.SkillInfo(s.getName(),
                                String.valueOf(s.getId())))
                        .collect(Collectors.toList());

        Long expertiseId = currentUser.getExpertise() != null ? currentUser.getExpertise().getId() : null;
        UpdateUserProfileForRecommendationDTO.ExpertiseInfo expertiseDetail = currentUser.getExpertise() != null
                ? new UpdateUserProfileForRecommendationDTO.ExpertiseInfo(currentUser.getExpertise().getName(),
                        String.valueOf(currentUser.getExpertise().getId()))
                : null;

        UpdateUserProfileForRecommendationDTO dto = new UpdateUserProfileForRecommendationDTO(skillIds,
                currentUser.getLevel(), expertiseId);
        dto.setSkillDetails(skillDetails);
        dto.setExpertiseDetail(expertiseDetail);
        return dto;
    }

}
