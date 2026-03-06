package vn.locpham.jobhunter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.dto.LoginDTO;
import vn.locpham.jobhunter.domain.dto.ResLoginDTO;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    @Value("${locpham.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshExpirition;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtils sercurityUtil;
    private final UserService userService;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtils sercurityUtil,
            UserService userService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.sercurityUtil = sercurityUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());

        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = new ResLoginDTO();
        User userCurrentDB = this.userService.handleGetUserByUsername(loginDTO.getUsername());
        if (userCurrentDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(userCurrentDB.getId(), userCurrentDB.getEmail(),
                    userCurrentDB.getName());
            res.setUser(userLogin);
        }
        String access_token = this.sercurityUtil.createAccessToken(authentication);
        res.setAccessToken(access_token);
        String refresh_token = this.sercurityUtil.createRefreshToken(loginDTO.getUsername(), res);

        // update user
        this.userService.updateUserToken(refresh_token, loginDTO.getUsername());

        // set cookie
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtRefreshExpirition)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }
}
