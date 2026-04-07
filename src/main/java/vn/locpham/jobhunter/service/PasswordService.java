package vn.locpham.jobhunter.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import vn.locpham.jobhunter.domain.PasswordReset;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.request.ReqChangePassword;
import vn.locpham.jobhunter.domain.request.ReqForgotPassword;
import vn.locpham.jobhunter.domain.request.ReqResetPassword;
import vn.locpham.jobhunter.domain.request.ReqVerifyOtp;
import vn.locpham.jobhunter.repository.PasswordRepository;
import vn.locpham.jobhunter.repository.UserRepository;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.error.IdInvalidException;

@Service
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRepository passwordRepository;
    private final EmailService emailService;

    public PasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordRepository passwordRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordRepository = passwordRepository;
        this.emailService = emailService;
    }

    private String generateOtp() {
        int otp = new Random().nextInt(1000000);
        return String.format("%06d", otp);
    }

    public void handleChangePassword(ReqChangePassword req) throws IdInvalidException {
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        User user = this.userRepository.findByEmail(email);
        String passDB = user.getPassword();
        if (!this.passwordEncoder.matches(req.getOldPassword(), passDB)) {
            throw new IdInvalidException("Mật khẩu không hợp lệ");
        }
        if (req.getNewPassword().equals(req.getOldPassword())) {
            throw new IdInvalidException("Không sử dụng lại mật khẩu cũ");
        }
        String hashPassword = this.passwordEncoder.encode(req.getNewPassword());
        user.setPassword(hashPassword);
        this.userRepository.save(user);
    }

    public void handleForgotPassword(ReqForgotPassword req) throws IdInvalidException {
        String mailReq = req.getEmail();
        User user = this.userRepository.findByEmail(mailReq);
        if (user == null) {
            throw new IdInvalidException("Email không hợp lệ");
        }
        String otp = this.generateOtp();
        PasswordReset reset = this.passwordRepository.findByUserId(user.getId())
                .orElse(new PasswordReset());
        reset.setUserId(user.getId());
        reset.setOtp(otp);
        reset.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        reset.setVeryfied(false);
        reset.setCreatedAt(LocalDateTime.now());
        this.passwordRepository.save(reset);
        this.emailService.sendOtpEmail(mailReq, "Mã OTP của bạn", "otp", otp);
    }

    public void handleVerifyOtp(ReqVerifyOtp req) throws IdInvalidException {
        User user = this.userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new IdInvalidException("Email không hợp lệ");
        }
        Optional<PasswordReset> op = this.passwordRepository.findByUserId(user.getId());
        PasswordReset pr = op.isPresent() ? op.get() : null;
        if (pr == null) {
            throw new IdInvalidException("Không hợp lệ");
        }
        if (pr.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP đã hết hạn");
        }
        if (pr.getOtp() == null || !pr.getOtp().equals(req.getOtp())) {
            throw new IdInvalidException("OTP không hợp lệ");
        }

        pr.setVeryfied(true);
        this.passwordRepository.save(pr);
    }

    @Transactional
    public void handleResetPassword(ReqResetPassword req) throws IdInvalidException {
        User user = this.userRepository.findByEmail(req.getEmail());
        if (user == null) {
            throw new IdInvalidException("Email không hợp lệ");
        }
        Optional<PasswordReset> op = this.passwordRepository.findByUserId(user.getId());
        PasswordReset pr = op.isPresent() ? op.get() : null;
        if (pr == null) {
            throw new IdInvalidException("Không hợp lệ");
        }
        if (pr.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IdInvalidException("OTP đã hết hạn");
        }
        if (!pr.isVeryfied()) {
            throw new IdInvalidException("Không hợp lệ");
        }
        if (this.passwordEncoder.matches(req.getNewPassword(), user.getPassword())) {
            throw new IdInvalidException("Không sử dụng lại mật khẩu cũ");
        }
        String hashPassword = this.passwordEncoder.encode(req.getNewPassword());
        user.setPassword(hashPassword);
        this.userRepository.save(user);
        this.passwordRepository.deleteByUserId(user.getId());
    }

}
