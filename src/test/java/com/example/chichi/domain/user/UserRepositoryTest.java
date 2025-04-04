package com.example.chichi.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("회원이 존재할 시 회원의 이메일로 회원 데이터를 가져온다.")
    void findByEmail() {
        //given
        String email = "testuser@gmail.com";
        String password = "123456";
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
        userRepository.save(user);

        //when
        Optional<User> foundUser = userRepository.findByEmail(email);

        //then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(email);
        assertThat(foundUser.get().getPassword()).isEqualTo(password);
    }

    @Test
    @DisplayName("회원이 존재할 시 True를 반환한다.")
    void existsByEmail() {
        //given
        String email = "testuser@gmail.com";
        String password = "123456";
        User user = User.builder()
                .email(email)
                .password(password)
                .build();
        userRepository.save(user);

        //when
        boolean exists = userRepository.existsByEmail(email);

        //then
        assertThat(exists).isTrue();
    }
}