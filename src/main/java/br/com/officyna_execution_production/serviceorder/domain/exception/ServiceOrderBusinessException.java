package br.com.officyna_execution_production.serviceorder.domain.exception;

public class ServiceOrderBusinessException extends RuntimeException {

    public ServiceOrderBusinessException(String message) {
        super(message);
    }
}