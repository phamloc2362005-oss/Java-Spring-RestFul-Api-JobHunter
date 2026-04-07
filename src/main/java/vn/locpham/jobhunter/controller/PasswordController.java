package vn.locpham.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.request.ReqChangePassword;
import vn.locpham.jobhunter.domain.request.ReqForgotPassword;
import vn.locpham.jobhunter.domain.request.ReqResetPassword;
import vn.locpham.jobhunter.domain.request.ReqVerifyOtp;
import vn.locpham.jobhunter.service.PasswordService;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class PasswordController {
    private final PasswordService passwordService;

    public PasswordController(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @PutMapping("/password/change")
    @ApiMessage("Change password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ReqChangePassword req) throws IdInvalidException {
        this.passwordService.handleChangePassword(req);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/password/forgot")
    @ApiMessage("Forgot password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ReqForgotPassword req) throws IdInvalidException {
        this.passwordService.handleForgotPassword(req);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/password/otp")
    public ResponseEntity<Void> verifyOtp(@Valid @RequestBody ReqVerifyOtp req) throws IdInvalidException {
        this.passwordService.handleVerifyOtp(req);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ReqResetPassword req) throws IdInvalidException {
        this.passwordService.handleResetPassword(req);
        return ResponseEntity.ok(null);
    }
}
