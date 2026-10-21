package com.bgsistemas.api.auth;

import com.bgsistemas.api.auth.dtos.LoginResponseDTO;
import com.bgsistemas.api.auth.dtos.RegisterRequestDTO;
import com.bgsistemas.api.auth.dtos.RegisterResponseDTO;
import com.bgsistemas.api.auth.exceptions.EmailAlreadyExistsException;
import com.bgsistemas.api.twofactor.PendingLoginService;
import com.bgsistemas.api.user.Role;
import com.bgsistemas.api.user.User;
import com.bgsistemas.api.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PendingLoginService pendingLogins;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                           PendingLoginService pendingLogins) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.pendingLogins = pendingLogins;
    }

    // The user arrives already authenticated (HTTP Basic): we only issue the token.
    // BGSI-276: with 2FA on, no JWT yet, only a pending token for the second step (POST /api/auth/login/2fa)
    @Override
    public LoginResponseDTO login(User user) {
        if (user.isTotpEnabled()) {
            return LoginResponseDTO.twoFactorPending(user.getEmail(), pendingLogins.start(user.getEmail(), Instant.now()));
        }
        // BGSI-277: shown in the list of administrators
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new LoginResponseDTO(token, user.getEmail(), user.getRole().name());
    }

    // Only reachable by an ADMIN (see SecurityConfig): creates another admin
    @Override
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        User user = new User(request.email(), passwordEncoder.encode(request.password()), Role.ADMIN);
        User saved = userRepository.save(user);
        return new RegisterResponseDTO(saved.getId(), saved.getEmail(), saved.getRole().name());
    }
}
