package llc.devlab.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        String email,
        String password,
        String displayName
) {
}
