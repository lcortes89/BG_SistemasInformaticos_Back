package com.bgsistemas.api.user;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

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

    // BGSI-302: "Administradores" with search box (email) + status

    @Test
    void shouldSearchAdminsByEmailAndStatus() {
        saveAdmin("ana@bg.es", UserStatus.ACTIVE);
        saveAdmin("ana.invited@bg.es", UserStatus.PENDING);
        saveAdmin("pedro@bg.es", UserStatus.BLOCKED);

        assertThat(emails(userRepository.search(Role.ADMIN, "%ana%", null, byEmail())))
                .containsExactly("ana.invited@bg.es", "ana@bg.es");
        assertThat(emails(userRepository.search(Role.ADMIN, "%ana%", UserStatus.PENDING, byEmail())))
                .containsExactly("ana.invited@bg.es");
        assertThat(emails(userRepository.search(Role.ADMIN, null, UserStatus.BLOCKED, byEmail())))
                .containsExactly("pedro@bg.es");
    }

    @Test
    void shouldTreatAdminsWithoutStatusAsActive() {
        User old = userRepository.save(new User("old@bg.es", "hashed", Role.ADMIN));
        saveAdmin("blocked@bg.es", UserStatus.BLOCKED);
        entityManager.flush();
        // Admins created before the column existed have NULL
        entityManager.createQuery("UPDATE User u SET u.status = NULL WHERE u.id = :id")
                .setParameter("id", old.getId())
                .executeUpdate();
        entityManager.clear();

        assertThat(emails(userRepository.search(Role.ADMIN, null, UserStatus.ACTIVE, byEmail())))
                .containsExactly("old@bg.es");
    }

    private void saveAdmin(String email, UserStatus status) {
        User user = new User(email, "hashed", Role.ADMIN);
        user.setStatus(status);
        userRepository.save(user);
    }

    private PageRequest byEmail() {
        return PageRequest.of(0, 10, Sort.by("email"));
    }

    private List<String> emails(Page<User> page) {
        return page.getContent().stream().map(User::getEmail).toList();
    }
}
