package llc.devlab.app.security;

import llc.devlab.app.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AccountPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new UnauthorizedException("Authentication context is unavailable.");
        }
        return principal;
    }
}
