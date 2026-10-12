package com.bgsistemas.api.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        userRepository.save(new User("admin@mail.com", "hashed", Role.ADMIN));

        assertThat(userRepository.findByEmail("admin@mail.com")).isPresent();
        assertThat(userRepository.findByEmail("other@mail.com")).isEmpty();
    }

    @Test
    void shouldCheckIfAnAdminExists() {
        assertThat(userRepository.existsByRole(Role.ADMIN)).isFalse();

        userRepository.save(new User("admin@mail.com", "hashed", Role.ADMIN));

        assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
        assertThat(userRepository.existsByEmail("admin@mail.com")).isTrue();
    }
}
