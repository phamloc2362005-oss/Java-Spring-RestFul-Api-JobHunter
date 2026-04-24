package vn.locpham.jobhunter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.domain.reponse.ResLoginDTO;
import vn.locpham.jobhunter.domain.reponse.user.ResCreateUserDTO;
import vn.locpham.jobhunter.domain.request.ReqLoginDTO;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.annotattion.ApiMessage;
import vn.locpham.jobhunter.util.error.IdInvalidException;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    // thời gian sống của refresh token, đọc từ file config
    @Value("${locpham.jwt.refresh-token-validity-in-seconds}")
    private long jwtRefreshExpirition;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtils sercurityUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder, SecurityUtils sercurityUtil,
            UserService userService, PasswordEncoder passwordEncoder) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.sercurityUtil = sercurityUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDTO) {

        // B1: tạo object chứa username + password người dùng gửi lên
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(), loginDTO.getPassword());

        // B2: đưa object này cho Spring Security để xác thực
        // Spring sẽ tự gọi loadUserByUsername(...) để lấy user từ DB rồi tự so password
        // bằng PasswordEncoder
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // B3: lưu thông tin user đã login vào SecurityContextđ ể request hiện tại biết
        // ai đang đăng nhập
        SecurityContextHolder.getContext().setAuthentication(authentication);
        // B4: tạo object response trả về FE
        ResLoginDTO res = new ResLoginDTO();
        // B5: lấy lại user từ DB để lấy thông tin id, email, name, role
        User userCurrentDB = this.userService.handleGetUserByUsername(loginDTO.getUsername());
        if (userCurrentDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    userCurrentDB.getId(),
                    userCurrentDB.getEmail(),
                    userCurrentDB.getName(),
                    userCurrentDB.getRole());
            res.setUser(userLogin);
        }
        // B6: tạo access token để FE dùng gọi các API protected
        String access_token = this.sercurityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(access_token);

        // B7: tạo refresh token để xin access token mới khi access token hết hạn
        String refresh_token = this.sercurityUtil.createRefreshToken(loginDTO.getUsername(), res);

        // B8: lưu refresh token vào DB
        // logout / refresh sau này sẽ cần check token này
        this.userService.updateUserToken(refresh_token, loginDTO.getUsername());

        // B9: set refresh token vào cookie HttpOnly
        // JS phía FE không đọc trực tiếp được cookie này
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtRefreshExpirition)
                .build();

        // B10: trả về body có access_token + user
        // và header có cookie refresh_token
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @GetMapping("/auth/account")
    @ApiMessage("fetch account")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
        // lấy email của user đang login từ SecurityContext
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        // lấy full thông tin user từ DB
        User userCurrentDB = this.userService.handleGetUserByUsername(email);
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();
        // build dữ liệu trả về
        if (userCurrentDB != null) {
            userLogin.setId(userCurrentDB.getId());
            userLogin.setEmail(userCurrentDB.getEmail());
            userLogin.setName(userCurrentDB.getName());
            userLogin.setRole(userCurrentDB.getRole());

            userGetAccount.setUser(userLogin);
        }
        return ResponseEntity.ok().body(userGetAccount);
    }

    @GetMapping("/auth/refresh")
    @ApiMessage("Get user by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token) throws IdInvalidException {
        // nếu không có cookie refresh_token thì báo lỗi
        if (refresh_token.equals("abc")) {
            throw new IdInvalidException("Ban can truyen vao Resfresh Token");
        }
        // check valid
        Jwt decodedToken;
        try {
            // B1: check refresh token có hợp lệ không
            decodedToken = this.sercurityUtil.checkValidRefreshToken(refresh_token);
        } catch (Exception e) {
            throw new IdInvalidException("Refresh Token khong hop le hoac da het han");
        }
        // B2: lấy email từ subject trong token
        String email = decodedToken.getSubject();
        // B3: check xem token này có đúng là token đang lưu trong DB của user không
        User currentUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
        if (currentUser == null) {
            throw new IdInvalidException("Refresh Token khong hop le hoac da het han");
        }
        // B4: build lại response user
        ResLoginDTO res = new ResLoginDTO();
        User userCurrentDB = this.userService.handleGetUserByUsername(email);
        if (userCurrentDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    userCurrentDB.getId(),
                    userCurrentDB.getEmail(),
                    userCurrentDB.getName(),
                    userCurrentDB.getRole());
            res.setUser(userLogin);
        }

        // B5: tạo access token mới
        String access_token = this.sercurityUtil.createAccessToken(email, res);
        res.setAccessToken(access_token);

        // B6: tạo refresh token mới
        String new_refresh_token = this.sercurityUtil.createRefreshToken(email, res);

        // B7: cập nhật refresh token mới vào DB
        this.userService.updateUserToken(new_refresh_token, email);

        // set cookie
        ResponseCookie resCookies = ResponseCookie.from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtRefreshExpirition)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("/auth/logout")
    @ApiMessage("Logout Complete")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        // B1: lấy email của user đang đăng nhập từ access token
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        // B2: xóa refresh token trong DB
        this.userService.handleDeleteRefreshTokenByEmail(email);
        if (email.equals("")) {
            throw new IdInvalidException("Access Token không hợp lệ");
        }
        // B3: tạo cookie refresh_token rỗng để xóa cookie bên trình duyệt
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", null)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .body(null);
    }

    @PostMapping("/auth/register")
    @ApiMessage("Register a new user")
    public ResponseEntity<ResCreateUserDTO> register(@Valid @RequestBody User postManUser) throws IdInvalidException {
        // B1: check email đã tồn tại chưa
        boolean isEmailExist = this.userService.isExistEmail(postManUser.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException(
                    "Email " + postManUser.getEmail() + "đã tồn tại, vui lòng sử dụng email khác.");
        }
        // B2: hash password trước khi lưu DB
        String hashPassword = this.passwordEncoder.encode(postManUser.getPassword());
        postManUser.setPassword(hashPassword);
        // B3: lưu user mới vào DB
        User User = this.userService.handleCreateUser(postManUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(User));
    }

}
