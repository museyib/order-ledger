package llc.devlab.app.security;

import lombok.RequiredArgsConstructor;
import llc.devlab.app.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accountRepository.findByEmailIgnoreCase(username)
                .map(AccountPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found."));
    }
}
