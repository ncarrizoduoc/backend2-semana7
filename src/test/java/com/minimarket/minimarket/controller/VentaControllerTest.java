package com.minimarket.minimarket.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.hamcrest.Matchers.hasSize;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.test.context.support.WithMockUser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.minimarket.entity.Categoria;
import com.minimarket.minimarket.entity.DetalleVenta;
import com.minimarket.minimarket.entity.Producto;
import com.minimarket.minimarket.entity.Rol;
import com.minimarket.minimarket.entity.Usuario;
import com.minimarket.minimarket.entity.Venta;
import com.minimarket.minimarket.security.config.SecurityConfig;
import com.minimarket.minimarket.security.monitor.SuspiciousActivityService;
import com.minimarket.minimarket.security.service.CustomUserDetailsService;
import com.minimarket.minimarket.security.util.JwtUtil;
import com.minimarket.minimarket.service.impl.VentaServiceImpl;

import static org.mockito.MockitoAnnotations.openMocks;

@WebMvcTest(VentaController.class)
@Import(SecurityConfig.class)
public class VentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentaServiceImpl ventaService;

    @MockitoBean
    private SuspiciousActivityService suspiciousActivityService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // Declaracion de objetos
    Rol rol;
    Set<Rol> roles;
    Usuario usuario;
    List<DetalleVenta> detalles;
    private Venta venta;

    // Se crean objetos (de clase Venta y otros) para probar las llamadas a endpoints
    @BeforeEach
    void setUp(){
        // Rol
        rol = new Rol(Long.valueOf(1), "CLIENTE", new HashSet<Usuario>());
        roles = new HashSet<>(Set.of(rol));
        //Usuario
        usuario = new Usuario(Long.valueOf(1), "UsuarioPrueba", "ContrasenaPrueba", roles);
        // Lista de detalles de ventas (vacia)
        detalles = new ArrayList<>();
        //Objeto Venta (para probar endpoint POST)
        venta = new Venta(Long.valueOf(1), usuario, Date.valueOf("2026-12-30"), detalles);
    }

    // Despues de cada prueba, se asigna null a los objetos para liberar espacio
    @AfterEach
    void tearDown(){
        rol = null;
        roles = null;
        usuario = null;
        detalles = null;
        venta = null;
    }

    // Prueba que verifica que un usuario con rol CAJERO pueda acceder al endpoint [POST /api/ventas] y
    // guardar una venta (debe incluir un RequestBody con una venta valida)
    @Test
    @WithMockUser(authorities = {"CAJERO"})
    public void cajeroPuedeGenerarVentaTest() throws Exception{
        // Arrange
        when(ventaService.save(any(Venta.class))).thenAnswer(invocation ->{
            return invocation.getArgument(0);
        });

        // Act y Assert
        mockMvc.perform(post("/api/ventas") // Se llama al endpoint [POST /api/ventas]
            .contentType(MediaType.APPLICATION_JSON) // Se envia un body formato Json
            .content(new ObjectMapper().writeValueAsString(venta))) // El body contiene un objeto Venta valido
            .andExpect(status().isOk()) // Verificar que retorna un status OK
            .andExpect(jsonPath("$.id").value(Long.valueOf(1))); // Verificar que el ID de la venta sea el esperado
    }

    // Prueba que valida que al llamar al endpoint [POST /api/ventas] con un usuario valido, pero
    // un body invalido (no corresponde a una venta), retorne un codigo de status 400 (Bad Request) 
    @Test
    @WithMockUser(authorities = {"CAJERO"})
    public void usuarioCajeroNoPuedeRegistrarVentaInvalidaTest() throws Exception{
        // Arrange
        String bodyInvalido = "{\"id\":}"; // String con body invalido (no corresponde a objeto Venta)

        mockMvc.perform(post("/api/ventas") // Se llama al endpoint [POST /api/ventas]
            .contentType(MediaType.APPLICATION_JSON)
            .content(bodyInvalido)) // Se envia el body no valido
            .andExpect(status().isBadRequest()); // Se espera un codigo 400 (Bad Request) como respuesta
    }

    // Prueba que verifica que un usuario con rol ADMIN no pueda acceder al endpoint [POST /api/ventas]
    // para registrar una venta, pues el endpoint solo admite usuarios con rol CAJERO
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void usuarioNoAutorizadoNoPuedeGuardarVentaTest() throws Exception{
        mockMvc.perform(post("/api/ventas") // Se llama al endpoint [POST /api/ventas]
            .contentType(MediaType.APPLICATION_JSON) // Se envia un body formato Json
            .content(new ObjectMapper().writeValueAsString(venta))) // El body contiene un objeto Venta valido
            .andExpect(status().isForbidden()); // Espera un Status 403 (prohibido)
    }

    // Prueba que verifica que un usuario con rol CAJERO pueda ver las ventas en sistema
    // llamando al endpoint [GET /api/ventas]
    @Test
    @WithMockUser(authorities = {"CAJERO"})
    public void cajeroPuedeVerVentaTest() throws Exception{
        // Arrange
        // Se crea una lista de ventas (con una venta)
        List<Venta> ventas = new ArrayList<Venta>(List.of(venta));
        // Al llamar al ventaService, debe retorna la lista de ventas
        when(ventaService.findAll()).thenReturn(ventas);

        // Assert
        mockMvc.perform(get("/api/ventas")) // Se llama al endpoint [GET /api/ventas]
            .andExpect(status().isOk()) // Se espera un codigo 200 (OK)
            .andExpect(jsonPath("$", hasSize(1))) // Se verifica que la lista de ventas tenga 1 elemento
            .andExpect(jsonPath("$[0].id").value(Long.valueOf(1))); // Se verifica que el ID del primer elemento sea 1
    }

    // Prueba que verifica que un usuario sin rol CAJERO no pueda ver el listado de ventas
    // llamando al endpoint [GET /api/ventas], pues se necesita rol CAJERO
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void noCajeroNoPuedeVerVentaTest() throws Exception{
        mockMvc.perform(get("/api/ventas")) // Se llama al endpoint [GET /api/ventas]
            .andExpect(status().isForbidden()); // Se espera un codigo 403 (Forbidden)
    }

}
