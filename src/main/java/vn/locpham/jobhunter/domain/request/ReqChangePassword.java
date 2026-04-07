package vn.locpham.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;

public class ReqChangePassword {
    @NotBlank(message = "Old password không được để trống")
    private String oldPassword;
    @NotBlank(message = "New password không được để trống")
    private String newPassword;

    public ReqChangePassword() {
    }

    public ReqChangePassword(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
