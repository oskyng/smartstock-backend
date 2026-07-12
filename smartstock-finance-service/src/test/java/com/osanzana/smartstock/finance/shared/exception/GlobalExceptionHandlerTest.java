package com.osanzana.smartstock.finance.shared.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("/api/test");
    }

    @Test
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFoundException(ex, webRequest);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException("Business error");
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(ex, webRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Business error", response.getBody().getMessage());
    }

    @Test
    void handleConflictException() {
        ConflictException ex = new ConflictException("Conflict");
        ResponseEntity<ErrorResponse> response = handler.handleConflictException(ex, webRequest);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorizedActionException() {
        UnauthorizedActionException ex = new UnauthorizedActionException("Unauthorized");
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorizedActionException(ex, webRequest);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(ex, webRequest);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");
        ResponseEntity<ErrorResponse> response = handler.handleAccessDeniedException(ex, webRequest);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No tiene permisos para acceder a este recurso", response.getBody().getMessage());
    }

    @Test
    void handleMissingRequestHeaderException() {
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("X-Test");
        
        ResponseEntity<ErrorResponse> response = handler.handleMissingRequestHeaderException(ex, webRequest);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El encabezado 'X-Test' es obligatorio", response.getBody().getMessage());
    }

    @Test
    void handleGlobalException() {
        Exception ex = new Exception("Internal error");
        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, webRequest);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ha ocurrido un error inesperado", response.getBody().getMessage());
    }

    @Test
    void handleValidationExceptions() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "field", "error message"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException((MethodParameter)null, bindingResult);
        
        ResponseEntity<ErrorResponse> response = handler.handleValidationExceptions(ex, webRequest);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validación fallida", response.getBody().getMessage());
        assertNotNull(response.getBody().getErrors());
        assertEquals("error message", response.getBody().getErrors().get("field"));
    }
}
