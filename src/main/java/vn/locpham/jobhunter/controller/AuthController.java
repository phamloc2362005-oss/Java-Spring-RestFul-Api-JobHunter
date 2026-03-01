package vn.locpham.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.dto.LoginDTO;
import vn.locpham.jobhunter.domain.dto.ResLoginDTO;
import vn.locpham.jobhunter.util.SercurityUtil;

@RestController
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SercurityUtil sercurityUtil;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SercurityUtil sercurityUtil) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.sercurityUtil = sercurityUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());

        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        String access_token = this.sercurityUtil.createToken(authentication);
        ResLoginDTO res = new ResLoginDTO();
        res.setAccessToken(access_token);
        return ResponseEntity.ok().body(res);
    }
}
