package com.minimarket.minimarket.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import com.minimarket.minimarket.security.monitor.SuspiciousActivityService;
import com.minimarket.minimarket.security.service.CustomUserDetailsService;
import com.minimarket.minimarket.security.util.JwtUtil;

import jakarta.servlet.FilterChain;

import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private SuspiciousActivityService suspiciousActivityService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter authFilter;

    // Declaracion de variables
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    

    @BeforeEach
    void setUp(){
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        
    }

    // Prueba que valida que el JwtAuthenticationFilter autentique al usuario
    // si el JWT adjunto a la solicitud HTTP es valido (el username en el JWT debe coincidir con
    // el de la base de datos y el token no debe estar expirado)
    @Test
    public void debeAutenticarCuandoJwtSeaValidoTest() throws Exception{
        // Arrange
        String token = "token_valido";
        String username = "username";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(jwtUtil.validateToken(token, username)).thenReturn(true);

        // Act
        authFilter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication(); // Se obtiene la informacion del usuario autenticado
        assertNotNull(auth); // Se valida que haya un usaurio autenticado (la autenticacion con JWT fue exitosa)
        assertInstanceOf(UsernamePasswordAuthenticationToken.class, auth);
        assertEquals(userDetails, auth.getPrincipal()); // Se verifican los UserDetails (username + password + autoridades)
        verify(suspiciousActivityService, times(1)).recordRequest(request);
    }

    // Prueba que valida que si el intento de autenticacion no incluye JWT no se autenticara al usuario
    @Test
    public void noDebeAutenticarSiRequestNoIncluyeJwtTest() throws Exception{
        // Act
        authFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication()); // Debe ser null porque no se autentico al usuario
        verify(suspiciousActivityService, times(1)).recordRequest(request);
    }

    // Prueba que valida que el JwtAuthenticationFilter no autentique al usuario si el JwtUtil
    // determina que el JWT adjuntado por el usuario no es valido
    @Test
    public void debeFallarAutenticacionSiJwtNoEsValidoTest() throws Exception{
        // Arrange
        String token = "token_valido";
        String username = "username";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        when(jwtUtil.validateToken(token, username)).thenReturn(false); // El JwtUtil 

        // Act 
        authFilter.doFilterInternal(request, response, filterChain);

        // Assert
        assertNull(SecurityContextHolder.getContext().getAuthentication()); // Debe ser null porque no se autentico al usuario
        verify(suspiciousActivityService, times(1)).recordRequest(request);
        verify(suspiciousActivityService, times(1)).recordInvalidJwt(request, token, null);

    }

}
