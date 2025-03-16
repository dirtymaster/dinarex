package dirtymaster.freedomexchange.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JdbcUserDetailsManager userDetailsService;
    private final ActiveService activeService;

    public boolean userExists(String username) {
        try {
            userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException ex) {
            return false;
        }
        return true;
    }

    public void registerUser(String username, String password) {
        // Код для создания пользователя
        String encodedPassword = passwordEncoder.encode(password);

        // Создание пользователя в базе данных
        userDetailsService.createUser(User.withUsername(username)
                .password(encodedPassword)
                .roles("USER")  // Присваиваем роль USER
                .build());
        activeService.createAllZeroActives(username);
    }

    public String getUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String name = authentication.getName();
            return "anonymousUser".equals(name) ? null : name;
        }
        return null;
    }

    public void deleteUser(String username) {
        userDetailsService.deleteUser(username);
    }
}
