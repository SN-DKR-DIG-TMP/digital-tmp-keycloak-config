package net.atos.digital.keycloak.exceptions;

public class ResourceAlreadyExistException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public ResourceAlreadyExistException(String entity, Object value) {
        super(entity + " in conflict with the value : " + value);
    }

    public ResourceAlreadyExistException(String entity, String realm, String fieldName, String value) {
        super(entity + " already exists for realm : " + realm + " and " + fieldName + " : " + fieldName);
    }

    public ResourceAlreadyExistException(String entity, String fieldName, String value) {
        super(entity + " already exists for " + fieldName + " : " + value);
    }

    public ResourceAlreadyExistException(String message) {
        super(message);
    }

}
