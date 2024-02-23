package net.atos.digital.keycloak.models;

import jakarta.validation.constraints.NotEmpty;

public record UserKeycloak(

        String userFirstName,

        String userLastName,

        String userEmailAddress,

        @NotEmpty
        String username,

        String userKeycloakId,

        String userLocale,

        String roleName,

        String userPassword,

        String userRealm

) {
}
