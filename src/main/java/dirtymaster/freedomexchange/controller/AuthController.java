package dirtymaster.freedomexchange.controller;

import dirtymaster.freedomexchange.security.SecurityConfiguration;
import dirtymaster.freedomexchange.service.ActiveService;
import dirtymaster.freedomexchange.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // Страница регистрации
    @GetMapping("/auth/register")
    public String registerForm() {
        return "auth/register"; // Возвращаем имя страницы регистрации
    }

    // Обработка регистрации
    @PostMapping("/auth/register")
    public String registerUser(@RequestParam String username, @RequestParam String password) {
        authService.registerUser(username, password);
        return "redirect:/auth/login";  // Перенаправляем на страницу входа после регистрации
    }

    // Страница входа
    @GetMapping("/auth/login")
    public String loginForm() {
        return "auth/login"; // Возвращаем страницу для входа
    }

    // Обработка регистрации
    @PostMapping("/auth/login")
    public String loginUser(@RequestParam String username, @RequestParam String password) {
        authService.registerUser(username, password);
        return "redirect:/";  // Перенаправляем на страницу входа после регистрации
    }

}