package vn.locpham.jobhunter.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ResLoginDTO {
    private String accessToken;
    private UserLogin user;

    public ResLoginDTO(String accessToken, UserLogin user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public ResLoginDTO() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public UserLogin getUser() {
        return user;
    }

    public void setUser(UserLogin user) {
        this.user = user;
    }

    public static class UserLogin {
        private long id;
        private String email;
        private String name;

        public UserLogin(long id, String email, String name) {
            this.email = email;
            this.id = id;
            this.name = name;
        }

        public UserLogin() {
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

}
