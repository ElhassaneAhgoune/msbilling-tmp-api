package com.msbillingauthapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.msbillingauthapi.testdata.TestUserBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryIT extends JpaTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_existingUser_returnsUser() {
        final var testUser = userRepository.save(TestUserBuilder.userBuilder().build());
        final var foundUser = userRepository.findByUsername(testUser.getUsername());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    void findByUsername_nonExistingUsername_returnsEmpty() {
        final var foundUser = userRepository.findByUsername("nonexistentUser");

        assertThat(foundUser).isNotPresent();
    }

    @Test
    void findByEmail_existingUser_returnsUser() {
        final var testUser = userRepository.save(TestUserBuilder.userBuilder().build());
        final var foundUser = userRepository.findByEmail(testUser.getEmail());

        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void findByEmail_nonExistingEmail_returnsEmpty() {
        final var foundUser = userRepository.findByEmail("nonexistent@example.com");

        assertThat(foundUser).isNotPresent();
    }

    @Test
    void existsByUsername_existingUsername_returnsTrue() {
        final var testUser = userRepository.save(TestUserBuilder.userBuilder().build());
        final var exists = userRepository.existsByUsername(testUser.getUsername());

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        final var testUser = userRepository.save(TestUserBuilder.userBuilder().build());
        final var exists = userRepository.existsByEmail(testUser.getEmail());

        assertThat(exists).isTrue();
    }

    @Test
    void saveAndFindById_user_returnsSavedUser() {
        final var newUser = TestUserBuilder.userBuilder().build();
        final var savedUser = userRepository.save(newUser);

        final var retrievedUser = userRepository.findById(savedUser.getId());

        assertThat(retrievedUser).isPresent();
        assertThat(retrievedUser.get().getUsername()).isEqualTo(newUser.getUsername());
        assertThat(retrievedUser.get().getEmail()).isEqualTo(newUser.getEmail());
    }

    @Test
    void delete_user_userDoesNotExist() {
        final var testUser = userRepository.save(TestUserBuilder.userBuilder().build());
        userRepository.delete(testUser);

        final var deletedUser = userRepository.findById(testUser.getId());

        assertThat(deletedUser).isNotPresent();
    }

}