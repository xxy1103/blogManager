package com.ulna.blog_manager.controller;

import com.ulna.blog_manager.Config.JwtUtils;
import com.ulna.blog_manager.model.User;
import com.ulna.blog_manager.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (userService.existsByUsername(registerRequest.getUsername())) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("错误: 用户名已被使用!"));
            }
            
            if (userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("错误: 邮箱已被使用!"));
            }
            
            User user = userService.createUser(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword()
            );
            
            return ResponseEntity.ok(new MessageResponse("用户注册成功!"));
        } catch (Exception e) {
            logger.error("注册失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new MessageResponse("注册失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(loginRequest.getUsername());
            
            Optional<User> userOptional = userService.findByUsername(loginRequest.getUsername());
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                return ResponseEntity.ok(new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail()));
            } else {
                return ResponseEntity.badRequest()
                    .body(new MessageResponse("用户不存在"));
            }
        } catch (Exception e) {
            logger.error("登录失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(new MessageResponse("登录失败: 用户名或密码错误"));
        }
    }
    
    // 内部类：请求和响应DTO
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    
    public static class MessageResponse {
        private String message;
        
        public MessageResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class JwtResponse {
        private String token;
        private String type = "Bearer";
        private Long id;
        private String username;
        private String email;
        
        public JwtResponse(String accessToken, Long id, String username, String email) {
            this.token = accessToken;
            this.id = id;
            this.username = username;
            this.email = email;
        }
        
        // Getters and Setters
        public String getAccessToken() { return token; }
        public void setAccessToken(String accessToken) { this.token = accessToken; }
        public String getTokenType() { return type; }
        public void setTokenType(String tokenType) { this.type = tokenType; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
