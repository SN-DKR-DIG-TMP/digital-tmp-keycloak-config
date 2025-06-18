package net.atos.digital.keycloak.services;

import net.atos.digital.keycloak.exceptions.ResourceAlreadyExistException;
import net.atos.digital.keycloak.mappers.EntityMapper;
import net.atos.digital.keycloak.models.UserDtoModel;
import net.atos.digital.keycloak.models.UserEntityModel;
import net.atos.digital.keycloak.models.UserKeycloak;
import net.atos.digital.keycloak.properties.UserManagerConfiguration;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
public class KeycloakUserService<E extends UserEntityModel, D extends UserDtoModel> {

    private final AbstractUserService<E, D> abstractUserService;
    private final EntityMapper<E, D> entityMapper;
    private final KeycloakService keycloakService;

    static final String USER = "user";
    static final String USERNAME_OR_EMAIL = "username/email";

    private final UserManagerConfiguration userManagerConfiguration;

    public KeycloakUserService(AbstractUserService<E, D> abstractUserService, EntityMapper<E, D> entityMapper, KeycloakService keycloakService, UserManagerConfiguration userManagerConfiguration) {
        this.abstractUserService = abstractUserService;
        this.entityMapper = entityMapper;
        this.keycloakService = keycloakService;
        this.userManagerConfiguration = userManagerConfiguration;
    }

    public D createUser(String keycloakRealm, String clientId, D user, boolean isTemporaryPassword) {

        UserKeycloak userKeycloak = abstractUserService.asUserKeycloak(user);

        /* Checking if resource already exists */
        if (abstractUserService.existsByUsername(userKeycloak.username())) {
            throw new ResourceAlreadyExistException(USER, USERNAME_OR_EMAIL, userKeycloak.username());
        }

        /* Checking if userEmailAddress is not already used */
        checkUserEmailAddressUnity(userKeycloak);

        /* Getting roleRepresentations */
        var roleRepresentation = keycloakService.getRoleRepresentationsByRealmAndNames(keycloakRealm, userKeycloak.roleName());

        /* Creating KEYCLOAK user */
        var keycloakUserIdentifier = keycloakService.createKeycloakUser(keycloakRealm, userKeycloak);

        log.info("User after keycloak end ok - keycloakUserIdentifier: {}", keycloakUserIdentifier);

        /* Save role into keycloak */
        if(Objects.nonNull(roleRepresentation)) {
            log.info(" Getting role end ok - keycloakUserIdentifier: {}", keycloakUserIdentifier);
            //only if roles are specified, if not, default roles are attributed by keycloak
            keycloakService.saveRoleRepresentationsInUser(keycloakRealm, keycloakUserIdentifier, Collections.singletonList(roleRepresentation));

            log.info(" Saving role end ok - keycloakUserIdentifier: {}", keycloakUserIdentifier);
        }

        /* If KEYCLOAK user password is filled */
        if (StringUtils.isNotEmpty(userKeycloak.userPassword())) {
            try {
                /* Setting KEYCLOAK user password */
                keycloakService.setKeycloakUserPassword(keycloakRealm, keycloakUserIdentifier, userKeycloak.userPassword(), isTemporaryPassword);
            } catch(Exception ex) {
                /* Deleting the user in keycloak server */
                keycloakService.deleteKeycloakUser(keycloakRealm, keycloakUserIdentifier);
                throw ex;
            }
        }

        log.info(" Setting user keycloakId  - keycloakUserIdentifier: {}", keycloakUserIdentifier);

        /* Setting user KEYCLOAK Identifier */
        user.setUserKeycloakId(keycloakUserIdentifier);

        log.info("Setting user keycloakId -- {}", keycloakUserIdentifier);

        /* Saving user */
        var savedUser = abstractUserService.saveUser(user);

        log.info("savedUser end ok - userId: {}", savedUser.getUserId());

        /* check email actions is active and user has an email address */
        if (userManagerConfiguration.isEmailActionsActive) {
            sendEmailActionsForPinPasswordSetting(savedUser, clientId);
            log.info("sending email end ok - userId: {}", savedUser.getUserId());
        }

        return savedUser;

    }

    public D updateUser(D user, boolean isTemporaryPassword) {

        UserKeycloak userKeycloak = abstractUserService.asUserKeycloak(user);

        /* Checking if userEmailAddress is not already used by another */
        checkUserEmailAddressUnity(userKeycloak, user.getUserId());

        /* Getting roleRepresentations */
        var roleRepresentation = keycloakService.getRoleRepresentationsByRealmAndNames(userKeycloak.userRealm(), userKeycloak.roleName());

        /* Updating KEYCLOAK user */
        keycloakService.updateKeycloakUser(userKeycloak.userRealm(), userKeycloak, isTemporaryPassword);

        /* Updating KEYCLOAK roles */
        keycloakService.saveRoleRepresentationsInUser(userKeycloak.userRealm(), userKeycloak.userKeycloakId(), Collections.singletonList(roleRepresentation));

        /* Saving user */
        var savedUser  = abstractUserService.saveUser(user);

        log.info("updateUser end ok - userId: {}", savedUser.getUserId());
        log.trace("updateUser end ok - user: {}", savedUser);

        return savedUser;

    }

    public D disableOrEnableKeycloakUser(D user) {

        UserKeycloak userKeycloak = abstractUserService.asUserKeycloak(user);

        /* Checking if userEmailAddress is not already used by another */
        checkUserEmailAddressUnity(userKeycloak, user.getUserId());

        /* Getting roleRepresentations */
        var roleRepresentation = keycloakService.getRoleRepresentationsByRealmAndNames(userKeycloak.userRealm(), userKeycloak.roleName());

        /* Updating KEYCLOAK user */
        keycloakService.disableOrEnableKeycloakUser(userKeycloak.userRealm(), userKeycloak.userKeycloakId(), user.isEnable());

        /* Updating KEYCLOAK roles */
        keycloakService.saveRoleRepresentationsInUser(userKeycloak.userRealm(), userKeycloak.userKeycloakId(), Collections.singletonList(roleRepresentation));

        /* Saving user */
        var savedUser  = abstractUserService.saveUser(user);

        log.info("disableOrEnableUser end ok - userId: {}", savedUser.getUserId());
        log.trace("disableOrEnableUser end ok - user: {}", savedUser);

        return savedUser;

    }

    private void checkUserEmailAddressUnity(UserKeycloak userKeycloak) {

        /* Checking if resource already exists */
        if (Objects.nonNull(userKeycloak.userEmailAddress()) && abstractUserService.existsByUserEmailAddress(userKeycloak.userEmailAddress())) {
            throw new ResourceAlreadyExistException(USER, USERNAME_OR_EMAIL, userKeycloak.userEmailAddress());
        }

    }

    private void checkUserEmailAddressUnity(UserKeycloak userKeycloak, Long userId) {

        /* Checking if resource already exists excluding email address owner */
        if (Objects.nonNull(userKeycloak.userEmailAddress()) && abstractUserService.existsByUserEmailAddressAndUserIdNot(userKeycloak.userEmailAddress(), userId)) {
            throw new ResourceAlreadyExistException(USER, USERNAME_OR_EMAIL, userKeycloak.userEmailAddress());
        }

    }

    private void sendEmailActionsForPinPasswordSetting(D savedUser, String clientId) {

        /* Initializing email actions list */
        var emailActionsList = List.of(userManagerConfiguration.emailActionsListString);

        try {

            /* Sending email for password setting */
            sendEmailActions(savedUser.getUserId(), clientId, emailActionsList);

        } catch(Exception ex) {

            /* Deleting the user in keycloak server */
            deleteUser(savedUser.getUserId());
            throw ex;
        }
    }

    public void deleteUser(Long userId) {

        /* Getting user */
        var user = abstractUserService.asUserKeycloak(abstractUserService.readUserByUserId(userId));

        /* Deleting KEYCLOAK user */
        keycloakService.deleteKeycloakUser(user.userRealm(), user.userKeycloakId());

        /* Deleting entity */
        abstractUserService.deleteUser(userId);

        log.info("deleteUser end ok - userId: {}", userId);
    }



    public void sendEmailActions(Long userId, String clientId, List<String> emailActionsList) {

        /* Getting user */
        var user = abstractUserService.asUserKeycloak(abstractUserService.readUserByUserId(userId));

        /* Getting KEYCLOAK connection instance */
        keycloakService.sendEmailActions(user.userRealm(), clientId, user.userKeycloakId(), emailActionsList);

        log.info("sendEmailActions end ok - keycloakUserIdentifier: {}", user.userKeycloakId());
        log.trace("sendEmailActions end ok - emailActionsList: {}", emailActionsList);
    }

    public void updatePassword(Long userId, String password) {

        /* Getting existing User */
        UserKeycloak userKeycloak = abstractUserService.asUserKeycloak(abstractUserService.readUserByUserId(userId));

        /* If KEYCLOAK user password is filled */
        if (StringUtils.isNotEmpty(password)) {
            /* Setting KEYCLOAK user password */
            keycloakService.setKeycloakUserPassword(userKeycloak.userRealm(), userKeycloak.userKeycloakId(), password, false);
        }

        log.info("updateUserCredentials end ok - userId: {}", userId);

    }

}
