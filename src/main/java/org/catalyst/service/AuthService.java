package org.catalyst.service;

import org.catalyst.model.User;
import org.catalyst.model.UserFactory;
import org.catalyst.model.UserType;
import org.catalyst.repo.UserRepository;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Validates sign-up data and coordinates the account factory, hasher, and repository. */
public final class AuthService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.dummyPasswordHash = passwordHasher.hash("catalyst-dummy-password".toCharArray());
    }

    public void signUp(
            String name,
            String email,
            String contactInfo,
            UserFactory userFactory,
            String organization,
            char[] password) throws SQLException {
        try {
            String cleanName = name == null ? "" : name.trim();
            String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
            String cleanContact = contactInfo == null ? "" : contactInfo.trim();
            String cleanOrganization = organization == null ? "" : organization.trim();

            if (cleanName.isBlank() || cleanName.length() > 150) {
                throw new IllegalArgumentException("Enter a name no longer than 150 characters.");
            }
            if (cleanEmail.length() > 255 || !EMAIL_PATTERN.matcher(cleanEmail).matches()) {
                throw new IllegalArgumentException("Enter a valid email address.");
            }
            if (cleanContact.length() > 255) {
                throw new IllegalArgumentException("Contact information must be 255 characters or fewer.");
            }
            if (userFactory == null) {
                throw new IllegalArgumentException("Choose an account type.");
            }
            if (userFactory.getUserType() != UserType.DONOR
                    && userFactory.getUserType() != UserType.VOLUNTEER) {
                throw new IllegalArgumentException("Staff accounts cannot be created through sign-up.");
            }
            if (cleanOrganization.length() > 200) {
                throw new IllegalArgumentException("Organization name must be 200 characters or fewer.");
            }
            if (password == null || password.length < 10 || password.length > 128) {
                throw new IllegalArgumentException("Your password must be between 10 and 128 characters.");
            }

            User user = userFactory.createUser(
                    cleanName, cleanEmail, cleanContact, cleanOrganization);
            userRepository.create(user, passwordHasher.hash(password));
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }

    public Optional<User> logIn(String email, char[] password) throws SQLException {
        try {
            if (email == null || email.isBlank() || password == null || password.length == 0) {
                return Optional.empty();
            }
            return userRepository.authenticate(
                    email.trim(), password, passwordHasher, dummyPasswordHash);
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
