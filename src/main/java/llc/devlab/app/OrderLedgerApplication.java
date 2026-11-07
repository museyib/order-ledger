package llc.devlab.app;

import llc.devlab.app.config.BootstrapAdminProperties;
import llc.devlab.app.config.PaginationProperties;
import llc.devlab.app.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableConfigurationProperties({
        JwtProperties.class,
        PaginationProperties.class,
        BootstrapAdminProperties.class
})
public class OrderLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderLedgerApplication.class, args);
    }

}
