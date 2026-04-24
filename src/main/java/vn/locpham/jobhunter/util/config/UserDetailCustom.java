package vn.locpham.jobhunter.util.config;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import vn.locpham.jobhunter.service.UserService;

@Component("userDetailsService")
public class UserDetailCustom implements UserDetailsService {

    private final UserService userService;

    public UserDetailCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Hàm này sẽ được Spring Security tự gọi khi login
        // username ở đây thực chất là email người dùng nhập vào
        vn.locpham.jobhunter.domain.User user = this.userService.handleGetUserByUsername(username);
        // Trả về object UserDetails cho Spring Security
        // gồm:
        // - email
        // - password đã hash trong DB
        // - danh sách quyền
        return new User(user.getEmail(), user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

}
