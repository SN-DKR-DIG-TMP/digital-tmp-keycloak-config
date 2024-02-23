package net.atos.digital.keycloak.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;



/**
 * filter values management class
 */
@Validated
@ConfigurationProperties(prefix = "keycloakadminclient")
public record KeycloakProperties(

        @Valid
        Realm master,

        @NotNull
        @Valid 
        Realm admin
) {

    
    
    public record Realm(

            @NotEmpty
            String realm,
            String clientId,
            String user,
            String password,
            String clientSecret
    ) { }
    
}