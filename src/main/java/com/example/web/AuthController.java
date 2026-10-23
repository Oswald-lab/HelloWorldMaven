package com.example.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.user.AuthService;
import com.example.user.OtpService;
import com.example.user.User;

@Controller
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/tasks";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String password,
                         HttpServletRequest request,
                         Model model) {
        try {
            User user = authService.register(email, password);
            startSession(request, user);
            return "redirect:/tasks";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "signup";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String requestCode(@RequestParam String email, Model model) {
        try {
            otpService.requestCode(email);
            model.addAttribute("email", email);
            model.addAttribute("ttlMinutes", otpService.getTtl().toMinutes());
            return "verify-login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/login/verify")
    public String verifyPage(@RequestParam String email, Model model) {
        model.addAttribute("email", email);
        model.addAttribute("ttlMinutes", otpService.getTtl().toMinutes());
        return "verify-login";
    }

    @PostMapping("/login/verify")
    public String login(@RequestParam String email,
                        @RequestParam String code,
                        HttpServletRequest request,
                        Model model) {
        model.addAttribute("email", email);
        model.addAttribute("ttlMinutes", otpService.getTtl().toMinutes());
        try {
            User user = otpService.verify(email, code);
            startSession(request, user);
            return "redirect:/tasks";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "verify-login";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }

    private void startSession(HttpServletRequest request, User user) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        request.getSession(true).setAttribute(AuthInterceptor.USER_ID, user.getId());
    }
}
