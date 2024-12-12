package net.atos.digital.keycloak.annotations;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "net.atos.digital.keycloak.*")
@ConfigurationPropertiesScan(basePackages = "net.atos.digital.keycloak.properties")
public class KeycloakConfigRegister {
}
