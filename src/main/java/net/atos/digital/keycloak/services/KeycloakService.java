package net.atos.digital.keycloak.services;

import net.atos.digital.keycloak.models.UserKeycloak;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

public interface KeycloakService {

    String createKeycloakUser(String keycloakRealm, UserKeycloak user);

    void updateKeycloakUser(String keycloakRealm, UserKeycloak user, boolean isTemporaryPassword);

    void deleteKeycloakUser(String keycloakRealm, String keycloakUserIdentifier);

    void setKeycloakUserPassword(String keycloakRealm, String keycloakUserIdentifier, String keycloakUserPassword, boolean temporary);

    void saveRoleRepresentationsInUser(String keycloakRealm, String keyCloakUserId, RoleRepresentation roleRepresentation);

    RoleRepresentation createKeycloakRole(String keycloakRealm, String roleName, String roleDescription);

    void deleteKeycloakRole(String keycloakRealm, String roleName);

    RoleRepresentation getRoleRepresentationsByRealmAndNames(String keycloakRealm, String name);

    void sendEmailActions(String keycloakRealm,  String clientId, String keycloakUserIdentifier, List<String> emailActionsList);

    void disableOrEnableKeycloakUser(String keycloakRealm, String userKeycloakId, boolean isEnable);
}
