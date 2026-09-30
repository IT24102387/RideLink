package lk.ac.ridelink.account_service.controller;

import jakarta.validation.Valid;
import lk.ac.ridelink.account_service.dto.RegisterRequest;
import lk.ac.ridelink.account_service.dto.LoginRequest;
import lk.ac.ridelink.account_service.dto.LoginResponse;
import lk.ac.ridelink.account_service.service.LoginService;
import lk.ac.ridelink.account_service.dto.UserResponse;
import lk.ac.ridelink.account_service.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final LoginService loginService;

    public AuthController(UserService userService, LoginService loginService) {
        this.userService = userService;
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(loginService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }
}
