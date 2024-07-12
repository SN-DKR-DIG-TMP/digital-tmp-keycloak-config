package net.atos.digital.keycloak.exceptions;


import jakarta.ws.rs.core.Response;
import net.atos.digital.keycloak.models.UserKeycloak;


public class KeycloakCreateUserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KeycloakCreateUserException(String realm, UserKeycloak user, Response.StatusType status) {
        super("An error occured when trying to process user [" + user + "] for realm [" + realm + "], error : " + status);
    }

}
