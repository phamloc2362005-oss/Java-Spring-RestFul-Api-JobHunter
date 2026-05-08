package vn.locpham.jobhunter.domain.request;

import java.util.List;

import vn.locpham.jobhunter.util.constant.LevelEnum;

/**
 * DTO để cập nhật user profile cho AI recommendation
 */
public class UpdateUserProfileForRecommendationDTO {
    private List<Long> skillIds; // IDs của skills user có
    private LevelEnum level; // Level/kinh nghiệm của user (INTERN, JUNIOR, MIDDLE, SENIOR)
    private Long expertiseId; // ID của expertise/chuyên ngành

    public UpdateUserProfileForRecommendationDTO() {
    }

    public UpdateUserProfileForRecommendationDTO(List<Long> skillIds, LevelEnum level, Long expertiseId) {
        this.skillIds = skillIds;
        this.level = level;
        this.expertiseId = expertiseId;
    }

    // Getters and Setters
    public List<Long> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<Long> skillIds) {
        this.skillIds = skillIds;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public void setLevel(LevelEnum level) {
        this.level = level;
    }

    public Long getExpertiseId() {
        return expertiseId;
    }

    public void setExpertiseId(Long expertiseId) {
        this.expertiseId = expertiseId;
    }
}
