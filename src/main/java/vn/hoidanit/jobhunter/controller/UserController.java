package vn.hoidanit.jobhunter.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.service.UserService;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // @GetMapping("/users/create")
    @PostMapping("/users/create")
    public ResponseEntity<User> createNewUser(@RequestBody User postmanUser) {
        User ericUser = this.userService.handleCreateUser(postmanUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ericUser);

    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable("id") Long id) {
        this.userService.handleDeleteUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("ericUser");
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> fetchUserById(@PathVariable("id") Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(this.userService.fetchUserById(id));

    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> fetchUser() {
        return ResponseEntity.status(HttpStatus.OK).body(this.userService.fetchAllUser());

    }

    @PutMapping("/users")
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        return ResponseEntity.ok(user);
    }
}
