package net.atos.digital.keycloak.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDtoModel {

    String userKeycloakId;

    Long userId;

    boolean enable;
}
