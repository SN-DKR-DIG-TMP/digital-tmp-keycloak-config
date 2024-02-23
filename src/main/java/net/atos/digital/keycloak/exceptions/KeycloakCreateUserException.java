package net.atos.digital.keycloak.exceptions;


import net.atos.digital.keycloak.models.UserKeycloak;

import javax.ws.rs.core.Response.StatusType;

public class KeycloakCreateUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KeycloakCreateUserException(String realm, UserKeycloak user, StatusType status) {
        super("An error occured when trying to process user [" + user + "] for realm [" + realm + "], error : " + status);
    }

}
