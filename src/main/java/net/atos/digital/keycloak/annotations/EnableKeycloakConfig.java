package net.atos.digital.keycloak.annotations;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(KeycloakConfigRegister.class)
public @interface EnableKeycloakConfig {
}
