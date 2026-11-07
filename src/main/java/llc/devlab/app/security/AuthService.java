package llc.devlab.app.security;

import java.util.Locale;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.exception.ConflictException;
import llc.devlab.app.exception.ResourceNotFoundException;
import llc.devlab.app.mapper.AccountMapper;
import llc.devlab.app.model.dto.AuthResponse;
import llc.devlab.app.model.dto.LoginRequest;
import llc.devlab.app.model.dto.LogoutRequest;
import llc.devlab.app.model.dto.RefreshTokenRequest;
import llc.devlab.app.model.dto.RegisterRequest;
import llc.devlab.app.model.entity.Account;
import llc.devlab.app.model.enums.AccountRole;
import llc.devlab.app.repository.AccountRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AccountMapper accountMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("An account with that email already exists.");
        }

        Account account = accountRepository.save(Account.builder()
                .email(normalizedEmail)
                .displayName(request.displayName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(AccountRole.USER)
                .build());

        return issueTokens(account);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));

        Account account = accountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated account was not found."));

        return issueTokens(account);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Account account = refreshTokenService.rotate(request.refreshToken());
        return issueTokens(account);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse issueTokens(Account account) {
        TokenEnvelope accessToken = jwtService.generateAccessToken(account);
        RefreshTokenEnvelope refreshToken = refreshTokenService.issue(account);
        return new AuthResponse(
                accessToken.token(),
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.expiresAt(),
                accountMapper.toSummary(account)
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
