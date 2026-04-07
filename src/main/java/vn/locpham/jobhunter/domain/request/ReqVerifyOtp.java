package vn.locpham.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;

public class ReqVerifyOtp {
    @NotBlank(message = "Email không được để trống")
    private String email;
    @NotBlank(message = "Otp không được để trống")
    private String otp;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

}
