package br.com.officyna_execution_production.administrative.supply.api.handler;

import br.com.officyna_execution_production.infrastructure.exception.ErrorResponse;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyBusinessException;
import br.com.officyna_execution_production.administrative.supply.domain.exception.SupplyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@org.springframework.core.annotation.Order(0)
@RestControllerAdvice
public class SupplyExceptionHandler {
    @ExceptionHandler(SupplyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SupplyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(SupplyBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(SupplyBusinessException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }
}
