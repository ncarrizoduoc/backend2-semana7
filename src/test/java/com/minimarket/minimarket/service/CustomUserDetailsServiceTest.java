package com.minimarket.minimarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.minimarket.minimarket.entity.Usuario;
import com.minimarket.minimarket.repository.UsuarioRepository;
import com.minimarket.minimarket.security.service.CustomUserDetailsService;

@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private CustomUserDetailsService detailsService;


    // Prueba que verifica que el metodo loadByUsername() retorne un objeto
    // CustomUserDetails si el nombre de usuario ingresado en el intento de autenticacion
    // corresponde a un usuario existente
    @Test
    public void retornaCustomUserDetailsSiUsuarioExisteTest(){
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setUsername("username");
        usuario.setPassword("password");
        when(usuarioRepo.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));

        // Act
        UserDetails detalles = detailsService.loadUserByUsername(usuario.getUsername());

        // Assert
        assertNotNull(detalles); // Verifica que el objeto retornado no sea null
        assertEquals(detalles.getUsername(), usuario.getUsername()); // Verifica que el CustomUserDetails tenga el username correcto
        assertEquals(detalles.getPassword(), usuario.getPassword()); // Verifica que el CustomUserDetails tenga el password correcto
        verify(usuarioRepo, times(1)).findByUsername(usuario.getUsername());
    }

    // Prueba que verifica que el metodo loadByUsername() lance una excepcion
    // UsernameNotFoundException si el nombre de usuario ingresado en el intento de autenticacion
    // no corresponde a ningun usuario registrado
    @Test
    public void lanzaExcepcionSiUsuarioNoExisteTest(){
        String username = "username";
        when(usuarioRepo.findByUsername(any(String.class))).thenReturn(Optional.empty());

        try {
            detailsService.loadUserByUsername(username); // Se espera que lance la excepcion UsernameNotFoundException
            fail("Se esperaba UsernameNotFoundException");
        } catch (UsernameNotFoundException e){
            assertEquals(e.getMessage(), "Usuario no encontrado: " + username); // Se verifica que el mensaje de la excepcion sea el esperado
            verify(usuarioRepo, times(1)).findByUsername(username);
        }
    }


}
