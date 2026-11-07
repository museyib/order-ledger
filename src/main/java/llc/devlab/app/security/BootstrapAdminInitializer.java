package llc.devlab.app.security;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.config.BootstrapAdminProperties;
import llc.devlab.app.model.entity.Account;
import llc.devlab.app.model.enums.AccountRole;
import llc.devlab.app.repository.AccountRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!StringUtils.hasText(bootstrapAdminProperties.email()) || !StringUtils.hasText(bootstrapAdminProperties.password())) {
            return;
        }

        if (accountRepository.existsByEmailIgnoreCase(bootstrapAdminProperties.email())) {
            return;
        }

        accountRepository.save(Account.builder()
                .email(bootstrapAdminProperties.email().trim().toLowerCase())
                .displayName(bootstrapAdminProperties.displayName())
                .passwordHash(passwordEncoder.encode(bootstrapAdminProperties.password()))
                .role(AccountRole.ADMIN)
                .build());
    }
}
