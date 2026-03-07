package vn.locpham.jobhunter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.annotattion.ApiMessage;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.dto.ReqLoginDTO;
import vn.locpham.jobhunter.domain.dto.ResLoginDTO;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.error.IdInvalidException;

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

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO) {
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
        String access_token = this.sercurityUtil.createAccessToken(authentication.getName(), res.getUser());
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

    @GetMapping("auth/account")
    @ApiMessage("fetch account")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        User userCurrentDB = this.userService.handleGetUserByUsername(email);
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();
        if (userCurrentDB != null) {
            userLogin.setId(userCurrentDB.getId());
            userLogin.setEmail(userCurrentDB.getEmail());
            userLogin.setName(userCurrentDB.getName());
            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok().body(userGetAccount);
    }

    @GetMapping("auth/refresh")
    @ApiMessage("Get user by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token) throws IdInvalidException {
        if (refresh_token.equals("abc")) {
            throw new IdInvalidException("Ban can truyen vao Resfresh Token");
        }
        Jwt decodedToken;
        try {
            decodedToken = this.sercurityUtil.checkValidrefreshToken(refresh_token);
        } catch (Exception e) {
            throw new IdInvalidException("Refresh Token khong hop le hoac da het han");
        }
        String email = decodedToken.getSubject();

        User currentUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
        if (currentUser == null) {
            throw new IdInvalidException("Refresh Token khong hop le hoac da het han");
        }

        ResLoginDTO res = new ResLoginDTO();
        User userCurrentDB = this.userService.handleGetUserByUsername(email);
        if (userCurrentDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(userCurrentDB.getId(), userCurrentDB.getEmail(),
                    userCurrentDB.getName());
            res.setUser(userLogin);
        }

        String access_token = this.sercurityUtil.createAccessToken(email, res.getUser());
        res.setAccessToken(access_token);
        String new_refresh_token = this.sercurityUtil.createRefreshToken(email, res);

        // update user
        this.userService.updateUserToken(new_refresh_token, email);

        // set cookie
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(jwtRefreshExpirition)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @GetMapping("auth/logout")
    @ApiMessage("Logout Complete")
    public ResponseEntity<Void> logout() {
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        this.userService.handleDeleteRefreshTokenByEmail(email);
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", null)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .body(null);
    }
}
