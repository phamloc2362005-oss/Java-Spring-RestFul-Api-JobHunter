package vn.locpham.jobhunter.domain.request;

import java.util.List;

import vn.locpham.jobhunter.util.constant.LevelEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO để cập nhật user profile cho AI recommendation
 */
public class UpdateUserProfileForRecommendationDTO {
    @JsonProperty("skillIds")
    private List<Long> skillIds; 
    
    @JsonProperty("level")
    private LevelEnum level; 
    
    @JsonProperty("expertiseId")
    private Long expertiseId; 

    @JsonProperty("skillDetails")
    public List<SkillInfo> skillDetails;

    @JsonProperty("expertiseDetail")
    public ExpertiseInfo expertiseDetail;

    public static class SkillInfo {
        private String label;
        private String value;

        public SkillInfo() {}

        public SkillInfo(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ExpertiseInfo {
        @JsonProperty("label")
        private String label;
        @JsonProperty("value")
        private String value;

        public ExpertiseInfo() {}

        public ExpertiseInfo(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public List<SkillInfo> getSkillDetails() {
        return skillDetails;
    }

    public void setSkillDetails(List<SkillInfo> skillDetails) {
        this.skillDetails = skillDetails;
    }

    public ExpertiseInfo getExpertiseDetail() {
        return expertiseDetail;
    }

    public void setExpertiseDetail(ExpertiseInfo expertiseDetail) {
        this.expertiseDetail = expertiseDetail;
    }

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
