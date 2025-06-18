package net.atos.digital.keycloak.services.impl;

import net.atos.digital.keycloak.exceptions.KeycloakCreateUserException;
import net.atos.digital.keycloak.exceptions.ResourceNotFoundException;
import net.atos.digital.keycloak.models.UserKeycloak;
import net.atos.digital.keycloak.services.KeycloakService;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeycloakServiceImpl implements KeycloakService {

    private static final String LOCALE_ATTRIBUTE = "locale";
    final Keycloak keycloak;


    public KeycloakServiceImpl(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    @Override
    public String createKeycloakUser(String keycloakRealm, UserKeycloak user) {

        /* Initializing KEYCLOAK "user" representation */
        var keycloakUser = new UserRepresentation();

        /* Setting KEYCLOAK "user" Username */
        keycloakUser.setUsername(user.username());

        /* Checking if userLastName is valued */
        if (Objects.nonNull(user.userLastName())) {

            /* Setting KEYCLOAK "user" Last Name */
            keycloakUser.setLastName(user.userLastName());

        }

        /* Checking if userFirstName is valued */
        if (Objects.nonNull(user.userFirstName())) {

            /* Setting KEYCLOAK "user" First Name */
            keycloakUser.setFirstName(user.userFirstName());

        }

        /* Checking if userEmailAddress is valued */
        if (Objects.nonNull(user.userEmailAddress())) {

            /* Setting KEYCLOAK "user" Email */
            keycloakUser.setEmail(user.userEmailAddress());
        }

        fillAttributes(user, keycloakUser);

        /* Enabling "user" */
        keycloakUser.setEnabled(true);

        /* Creating KEYCLOAK "user" */
        var response = keycloak.realm(keycloakRealm).users().create(keycloakUser);

        /* Checking response status */
        if (response.getStatus() != 201) {
            log.error("createKeycloakUser - error for - keycloakRealm: {} - user: {} - User not created : {}", keycloakRealm, user, response.getStatusInfo());
            /* avoid NPE somewhat lower */
            throw new KeycloakCreateUserException(keycloakRealm, user, response.getStatusInfo());
        }

        /* Getting KEYCLOAK "user" identifier */
        var keycloakUserIdentifier = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        log.info("createKeycloakUser end ok - keycloakRealm: {} - keycloakUserIdentifier: {}", keycloakRealm, keycloakUserIdentifier);
        log.trace("createKeycloakUser end ok - response: {}", response);

        return keycloakUserIdentifier;
    }

    @Override
    public void updateKeycloakUser(String keycloakRealm, UserKeycloak user, boolean isTemporaryPassword) {

        /* Getting KEYCLOAK "user" representation */
        var keycloakUser = keycloak.realm(keycloakRealm).users().get(user.userKeycloakId()).toRepresentation();

        /* Setting KEYCLOAK "user" Last Name */
        keycloakUser.setLastName(user.userLastName());

        /* Setting KEYCLOAK "user" First Name */
        keycloakUser.setFirstName(user.userFirstName());

        /* Getting userEmailAddress */
        var userEmailAddress = user.userEmailAddress();

        /* Checking userEmailAddress is valued */
        if (userEmailAddress != null) {

            /* Setting KEYCLOAK "user" Email */
            keycloakUser.setEmail(userEmailAddress);
        }

        fillAttributes(user, keycloakUser);

        /* Getting KEYCLOAK "user" resource */
        var keycloakUserResource = keycloak.realm(keycloakRealm).users().get(user.userKeycloakId());

        /* Updating KEYCLOAK "user" */
        keycloakUserResource.update(keycloakUser);

        /* If user password valued */
        if (user.userPassword() != null) {
            /* Resetting password */
            setKeycloakUserPassword(keycloakRealm, user.userKeycloakId(), user.userPassword(), isTemporaryPassword);
        }

        log.info("updateKeycloakUser end ok  - keycloakUserIdentifier: {}", user.userKeycloakId());
        log.trace("updateKeycloakUser end ok - user: {}", user);

    }

    @Override
    public void deleteKeycloakUser(String keycloakRealm, String keycloakUserIdentifier) {

        /* Getting KEYCLOAK "user" resource */
        var keycloakUserResource = keycloak.realm(keycloakRealm).users().get(keycloakUserIdentifier);

        /* Deleting KEYCLOAK "user" resource */
        keycloakUserResource.remove();

        log.info("deleteKeycloakUser end ok  - keycloakUserIdentifier: {}", keycloakUserIdentifier);

    }

    @Override
    public void setKeycloakUserPassword(String keycloakRealm, String keycloakUserIdentifier, String keycloakUserPassword, boolean temporary) {

        /* Getting KEYCLOAK "user" resource */
        var keycloakUserResource = keycloak.realm(keycloakRealm).users().get(keycloakUserIdentifier);

        /* Creating KEYCLOAK "user" credentials */
        var passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(temporary);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(keycloakUserPassword);

        /* Resetting KEYCLOAK "user" password */
        keycloakUserResource.resetPassword(passwordCred);

        log.info("setKeycloakUserPassword end ok  - keycloakUserIdentifier: {} - keycloakUserPassword: ####", keycloakUserIdentifier);

    }

    @Override
    public void saveRoleRepresentationsInUser(String keycloakRealm, String keyCloakUserId, List<RoleRepresentation> roleRepresentations) {

        List<RoleRepresentation> currentRoles = keycloak.realm(keycloakRealm).users().get(keyCloakUserId).roles().realmLevel().listAll();
        /* Remove current role*/
        keycloak.realm(keycloakRealm).users().get(keyCloakUserId).roles().realmLevel().remove(currentRoles);

        var representations = new ArrayList<RoleRepresentation>();

        /* Add keycloak default role for realm*/
        var defaultRole = getRoleRepresentationsByRealmAndNames(keycloakRealm, "default-roles-" + keycloakRealm.toLowerCase());

        representations.add(defaultRole);
        representations.addAll(roleRepresentations);

        keycloak.realm(keycloakRealm).users().get(keyCloakUserId).roles().realmLevel().add(representations);


        log.info("saveRoleRepresentationsInUser end ok - keyCloakUserId: {}", keyCloakUserId);
    }

    @Override
    public RoleRepresentation createKeycloakRole(String keycloakRealm, String roleName, String roleDescription) {

        RoleRepresentation roleRepresentation = new RoleRepresentation(roleName, roleDescription, false);
        keycloak.realm(keycloakRealm).roles();
        keycloak.realm(keycloakRealm).roles().create(roleRepresentation);


        roleRepresentation = keycloak.realm(keycloakRealm).roles().get(roleName).toRepresentation();

        log.info("createKeycloakRole end ok  - roleName: {} - roleDescription: {}", roleName, roleDescription);
        log.trace("createKeycloakRole end ok - roleRepresentation: {}", roleRepresentation);

        return roleRepresentation;
    }

    @Override
    public void deleteKeycloakRole(String keycloakRealm, String roleName) {

        keycloak.realm(keycloakRealm).roles().deleteRole(roleName);

        log.info("deleteKeycloakRole end ok  - roleName: {}", roleName);
        log.trace("deleteKeycloakRole end ok - roleName: {}", roleName);
    }

    @Override
    public RoleRepresentation getRoleRepresentationsByRealmAndNames(String keycloakRealm, String name) {

        var realmRoles = keycloak.realm(keycloakRealm).roles().list();

        return realmRoles.stream()
                .filter(rp -> rp.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new ResourceNotFoundException("role", name));

    }

    @Override
    public void sendEmailActions(String keycloakRealm, String clientId, String keycloakUserIdentifier, List<String> emailActionsList) {

        /* Getting client resource list */
        var keycloakClientRepresentationsList = keycloak.realm(keycloakRealm).clients().findByClientId(clientId);

        /* Checking if client resource list is not empty */
        if (!keycloakClientRepresentationsList.isEmpty()) {

            /* Setting client resource */
            var keycloakClientRepresentation = keycloakClientRepresentationsList.getFirst();

            /* Getting KEYCLOAK "user" resource */
            var keycloakUserResource = keycloak.realm(keycloakRealm).users().get(keycloakUserIdentifier);

            /* Sending email actions */
            keycloakUserResource.executeActionsEmail(keycloakClientRepresentation.getClientId(), keycloakClientRepresentation.getBaseUrl(), emailActionsList);

        } else {

            log.error("Keycloak client representation not found for client ID  {}.", clientId);

        }

        log.info("sendEmailActions end ok - keycloakClientId: {}, keycloakUserIdentifier: {}", clientId, keycloakUserIdentifier);
        log.trace("sendEmailActions end ok - emailActionsList: {}", emailActionsList);

    }

    @Override
    public void disableOrEnableKeycloakUser(String keycloakRealm, String userKeycloakId, boolean toEnable) {

        /* Getting KEYCLOAK "user" representation */
        var keycloakUser = keycloak.realm(keycloakRealm).users().get(userKeycloakId).toRepresentation();

        /* Enable or disable user */
        keycloakUser.setEnabled(toEnable);

        /* Getting KEYCLOAK "user" resource */
        var keycloakUserResource = keycloak.realm(keycloakRealm).users().get(userKeycloakId);

        /* Updating KEYCLOAK "user" */
        keycloakUserResource.update(keycloakUser);

        log.info("DisableOrEnableKeycloakUser end ok  - keycloakUserIdentifier: {}", userKeycloakId);
        log.trace("DisableOrEnableKeycloakUser end ok - user: {}", keycloakUser);
    }

    private void fillAttributes(UserKeycloak user, UserRepresentation keycloakUser) {

        if (StringUtils.isNotEmpty(user.userLocale())) {
            keycloakUser.singleAttribute(LOCALE_ATTRIBUTE, user.userLocale());
        }
    }

}
