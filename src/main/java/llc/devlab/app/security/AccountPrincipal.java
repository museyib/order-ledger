package llc.devlab.app.security;

import java.util.Collection;
import java.util.List;

import llc.devlab.app.model.entity.Account;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountPrincipal(Long id, String email, String displayName, String passwordHash,
                               String role) implements UserDetails {

    public static AccountPrincipal from(Account account) {
        return new AccountPrincipal(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getPasswordHash(),
                account.getRole().name()
        );
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @NonNull
    public String getUsername() {
        return email;
    }
}
