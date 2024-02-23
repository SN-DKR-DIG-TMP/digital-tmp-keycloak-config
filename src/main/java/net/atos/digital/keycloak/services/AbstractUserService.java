package net.atos.digital.keycloak.services;

import net.atos.digital.keycloak.models.UserDtoModel;
import net.atos.digital.keycloak.models.UserEntityModel;
import net.atos.digital.keycloak.models.UserKeycloak;

public interface AbstractUserService <E extends UserEntityModel, D extends UserDtoModel> {

    UserKeycloak asUserKeycloak(D userDto);

    boolean existsByUsername(String username);

    boolean existsByUserEmailAddress(String userEmailAddress);

    D saveUser(D userDto);

    D readUserByUserId(Long userId);

    D deleteUser(Long userId);

    boolean existsByUserEmailAddressAndUserIdNot(String userEmailAddress,Long userId);
}
