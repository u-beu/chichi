package com.example.chichi.domain.user;

import com.example.chichi.exception.ApiException;
import com.example.chichi.exception.ExceptionType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.example.chichi.exception.ExceptionType.USER_ALREADY_EXISTS;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void join(String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(USER_ALREADY_EXISTS);
        }
        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .build());
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ApiException(ExceptionType.USER_NOT_FOUND));
        String savedPassword = user.getPassword();
        if (passwordEncoder.matches(currentPassword, savedPassword)) {
            user.updatePassword(passwordEncoder.encode(newPassword));
        } else {
            throw new ApiException(ExceptionType.CURRENT_PASSWORD_MISMATCH);
        }
    }
}
