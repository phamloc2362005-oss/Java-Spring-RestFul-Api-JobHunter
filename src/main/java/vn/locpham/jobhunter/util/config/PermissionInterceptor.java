package vn.locpham.jobhunter.util.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.locpham.jobhunter.domain.Permission;
import vn.locpham.jobhunter.domain.Role;
import vn.locpham.jobhunter.domain.User;
import vn.locpham.jobhunter.service.UserService;
import vn.locpham.jobhunter.util.SecurityUtils;
import vn.locpham.jobhunter.util.error.IdInvalidException;
import vn.locpham.jobhunter.util.error.PermissionException;

public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Override
    @Transactional
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response, Object handler)
            throws Exception {
        // B1: lấy path pattern mà Spring match được
        // ví dụ request /api/v1/users/1 có thể match thành /api/v1/users/{id}
        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        // URI thật user gọi
        String requestURI = request.getRequestURI();
        // method của request: GET / POST / PUT / DELETE
        String httpMethod = request.getMethod();

        // log ra để debug
        System.out.println(">>> RUN preHandle");
        System.out.println(">>> path= " + path);
        System.out.println(">>> httpMethod= " + httpMethod);
        System.out.println(">>> requestURI= " + requestURI);
        // B2: lấy email của user đang đăng nhập
        // email này được lấy từ SecurityContext (token đã decode trước đó)
        String email = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get() : "";
        // nếu có email => nghĩa là đã xác thực thành công
        if (email != null && !email.isEmpty()) {

            // B3: lấy user hiện tại từ DB
            User user = userService.handleGetUserByUsername(email);
            if (user != null) {
                Role role = user.getRole();
                if (role != null) {
                    // B4: lấy role của user
                    List<Permission> permissions = role.getPermissions();
                    // B6: kiểm tra xem role có quyền với API hiện tại không
                    // điều kiện khớp:
                    // - path của permission == path request
                    // - method của permission == method request
                    boolean isAllow = permissions.stream()
                            .anyMatch(item -> item.getApiPath().equals(path) &&
                                    item.getMethod().equals(httpMethod));
                    if (isAllow == false) {
                        // user không có role hợp lệ
                        throw new PermissionException("You are not allowed to access this resource");
                    }
                } else {
                    // B8: nếu pass hết check thì cho request đi tiếp vào controller
                    throw new PermissionException("Role of user is invalid");
                }
            }
        }
        // B8: nếu pass hết check thì cho request đi tiếp vào controller
        return true;
    }
}