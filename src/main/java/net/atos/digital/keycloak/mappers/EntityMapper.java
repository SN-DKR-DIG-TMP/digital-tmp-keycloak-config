package net.atos.digital.keycloak.mappers;

import net.atos.digital.keycloak.models.UserDtoModel;
import net.atos.digital.keycloak.models.UserEntityModel;

public interface EntityMapper<E extends UserEntityModel, D extends UserDtoModel> {

    E asEntity(D dto);
    D asDto(E e);
}
