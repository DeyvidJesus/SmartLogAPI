package br.com.smartlog.domain.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " não encontrada: " + id);
    }
}
