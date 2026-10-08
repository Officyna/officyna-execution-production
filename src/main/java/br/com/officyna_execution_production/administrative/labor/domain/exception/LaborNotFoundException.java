package br.com.officyna_execution_production.administrative.labor.domain.exception;

public class LaborNotFoundException extends RuntimeException {

    public LaborNotFoundException(String message) {
        super(message);
    }

    public static LaborNotFoundException of(Object id) {
        return new LaborNotFoundException("Labor not found with id: " + id);
    }
}
