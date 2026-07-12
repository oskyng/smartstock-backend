package com.osanzana.smartstock.finance.shared.security;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.mockito.Mockito.*;

class SecurityHandlersTest {

    @Test
    void customAuthenticationEntryPoint_Commence() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);
        
        when(request.getRequestURI()).thenReturn("/api/test");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new FakeServletOutputStream(baos);
        when(response.getOutputStream()).thenReturn(sos);

        entryPoint.commence(request, response, authException);

        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
    }

    @Test
    void customAccessDeniedHandler_Handle() throws Exception {
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AccessDeniedException accessDeniedException = mock(AccessDeniedException.class);

        when(request.getRequestURI()).thenReturn("/api/test");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ServletOutputStream sos = new FakeServletOutputStream(baos);
        when(response.getOutputStream()).thenReturn(sos);

        handler.handle(request, response, accessDeniedException);

        verify(response).setStatus(403);
        verify(response).setContentType("application/json");
    }

    private static class FakeServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream baos;
        public FakeServletOutputStream(ByteArrayOutputStream baos) { this.baos = baos; }
        @Override public void write(int b) throws IOException { baos.write(b); }
        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(WriteListener writeListener) {}
    }
}
