package com.minimarket.minimarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimarket.minimarket.entity.Categoria;
import com.minimarket.minimarket.entity.Producto;
import com.minimarket.minimarket.security.config.SecurityConfig;
import com.minimarket.minimarket.security.monitor.SuspiciousActivityService;
import com.minimarket.minimarket.security.service.CustomUserDetailsService;
import com.minimarket.minimarket.security.util.JwtUtil;
import com.minimarket.minimarket.service.impl.ProductoServiceImpl;

@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
public class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoServiceImpl productoService;

    @MockitoBean
    private SuspiciousActivityService suspiciousActivityService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void setUp(){
        categoria = new Categoria(Long.valueOf(1), "Abarrotes", new ArrayList<Producto>());
        producto = new Producto(Long.valueOf(1), "Arroz", 12990.0, 10, categoria);
    }

    @AfterEach
    void tearDown(){
        categoria = null;
        producto = null;
    }

    // Prueba que valida que un usuario autorizado (con rol ADMIN) pueda acceder al endpoint
    // [PUT /api/productos] para editar un producto
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void usuarioAutorizadoPuedeModificarProductoTest() throws Exception{
        // Arrange
        when(productoService.findById(Long.valueOf(99))).thenReturn(new Producto());
        when(productoService.save(any(Producto.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        mockMvc.perform(put("/api/productos/{id}", Long.valueOf(99)) // Se llama al endpoint [PUT /api/productos/99]
            .contentType(MediaType.APPLICATION_JSON) // Se envia un body formato Json
            .content(new ObjectMapper().writeValueAsString(producto))) // El body contiene un objeto Producto valido
            .andExpect(status().isOk()) // Se espera un codigo 200 (OK)
            .andExpect(jsonPath("$.id").value(Long.valueOf(99))) // Se valida que el Producto retornado tenga ID 99
            .andExpect(jsonPath("$.nombre").value("Arroz")); // Se valida que el nombre del producto sea el esperado
    }

    // Prueba que valida que un usuario autorizado (con rol ADMIN) llama al endpoint [PUT /api/productos/{id}]
    // para modificar un producto que no existe (por ID), recibe un status Not Found
    @Test
    @WithMockUser(authorities = {"ADMIN"}) 
    public void respondeNotFoundSiProductoModificadoNoExisteTest() throws Exception{
        when(productoService.findById(Long.valueOf(99))).thenReturn(null);

        mockMvc.perform(put("/api/productos/{id}", Long.valueOf(99)) // Se llama al endpoint [PUT /api/productos/99]
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(producto)))
            .andExpect(status().isNotFound()); // Se espera un status Not Found

    }


    // Prueba que valida que un usuario no autorizado (sin rol ADMIN) no pueda acceder al endpoint
    // [PUT /api/productos/{id}] para editar un producto
    @Test
    @WithAnonymousUser
    public void usuarioNoAutorizadoNoPuedeModificarProductoTest() throws Exception{
        mockMvc.perform(put("/api/productos/{id}", Long.valueOf(99)) // Se llama al endpoint [PUT /api/productos/99]
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(producto)))
            .andExpect(status().isForbidden()); // Se espera un codigo 403 (Forbidden)
    }

    // Prueba que verifica que el endpoint [GET /api/productos] sea publico
    // Se hace la prueba con un usuario anonimo (sin rol)
    @Test
    @WithAnonymousUser
    public void cualquierUsuarioPuedeVerProductosTest() throws Exception{
        // Arrange
        List<Producto> productos = new ArrayList<Producto>(List.of(producto));
        when(productoService.findAll()).thenReturn(productos);

        // Assert
        mockMvc.perform(get("/api/productos")) // Se llama al endpoint [GET /api/productos]
            .andExpect(status().isOk()) // Se espera un codigo 200 (OK)
            .andExpect(jsonPath("$", hasSize(1))) // Se verifica que la lista de productos retornada tenga 1 elemento
            .andExpect(jsonPath("$[0].id").value(Long.valueOf(1))); // Se verifica que el ID del elemento en la lista sea 1
    }

    // Prueba que valida que el endpoint publico [GET /api/productos] retorne
    // un producto buscado por su ID, si existe
    @Test
    @WithAnonymousUser
    public void buscarProductoPorIdRetornaProductoSiExisteTest() throws Exception{
        // Arrange
        when(productoService.findById(Long.valueOf(1))).thenReturn(producto);

        // Assert
        mockMvc.perform(get("/api/productos/{id}", Long.valueOf(1))) // Se llama al endpoint [GET /api/productos/1]
            .andExpect(status().isOk()) // Se espera un codigo 200 (OK)
            .andExpect(jsonPath("$.id").value(Long.valueOf(1))) // Se valida el ID del producto encontrado
            .andExpect(jsonPath("$.nombre").value("Arroz")); // Se valida el nombre del producto encontrado
    }

    // Prueba que valida que el endpoint publico [GET /api/productos] retorne
    // un body vacio si el producto con ID buscado no existe
    @Test
    @WithAnonymousUser
    public void buscarProductoPorIdRetornaNullSiNoExisteTest() throws Exception{
        // Arrange
        when(productoService.findById(Long.valueOf(1))).thenReturn(null);

        // Assert
        mockMvc.perform(get("/api/productos/{id}", Long.valueOf(1))) // Se llama al endpoint [GET /api/productos/1]
            .andExpect(status().isNotFound()) // Se espera un status Not Found
            .andExpect(content().string("")); // Se espera un body de respuesta vacio
            
    }

    // Prueba que valida que un usuario autorizado (con rol ADMIN) acceder al
    // endpoint [POST /api/productos] para guardar un producto
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void usuarioAutorizadoPuedeGuardarProductoTest() throws Exception{
        // Arrange
        when(productoService.save(any(Producto.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });

        // Act y Assert
        mockMvc.perform(post("/api/productos") // Se llama al endpoint [POST /api/productos]
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(producto)))
            .andExpect(status().isOk()) // Espera un codigo 200 (OK)
            .andExpect(jsonPath("$.id").value(Long.valueOf(1))) // Valida el ID del producto retornado
            .andExpect(jsonPath("$.nombre").value("Arroz")); // Valida el nombre del producto retornado

    }

    // Prueba que valida que un usuario no autorizado (sin rol ADMIN) no pueda acceder
    // al endpoint [POST /api/productos]
    @Test
    @WithAnonymousUser
    public void usuarioNoAutorizadoNoPuedeGuardarProductoTest() throws Exception{
        mockMvc.perform(post("/api/productos") // Se llama al endpoint [POST /api/productos]
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(producto)))
            .andExpect(status().isForbidden()); // Espera un status Forbidden
    }

    // Prueba que valida que un usuario autorizado (con rol ADMIN) pueda acceder al endpoint
    // [DELETE /api/productos] para eliminar un producto por ID, si el producto existe
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void usuarioAutorizadoPuedeEliminarProductoSiExisteTest() throws Exception{
        // Arrange
        when(productoService.findById(Long.valueOf(1))).thenReturn(producto);

        // Act y Assert
        mockMvc.perform(delete("/api/productos/{id}", Long.valueOf(1))) // Se llama al endpoint [DELETE /api/productos/1]
            .andExpect(status().isNoContent()); // Espera un status No Content
    }

    // Prueba que valida que si un usuario autorizado (con rol ADMIN) intenta eliminar un producto
    // que no existe, a traves del endpoint [DELETE /api/productos/{id}], recibe un status Not Found
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void eliminarProductoRetornaNotFoundSiNoExisteTest() throws Exception{
        // Arrange
        when(productoService.findById(Long.valueOf(1))).thenReturn(null);

        // Act y Assert
        mockMvc.perform(delete("/api/productos/{id}", Long.valueOf(1))) // Se llama al endpoint [DELETE /api/productos/1]
            .andExpect(status().isNotFound()); // Espera un status Not Found
    }

    // Prueba que valida que un usuario no autorizado no pueda acceder al endpoint [DELETE /api/productos/{id}]
    @Test
    @WithMockUser(authorities = {"NOAUTORIZADO"})
    public void usuarioNoAutorizadoNoPuedeEliminarProductoTest() throws Exception{
        mockMvc.perform(delete("/api/productos/{id}", Long.valueOf(1))) // Llama al endpoint [DELETE /api/productos/1]
            .andExpect(status().isForbidden()); // Espera un status Forbidden
    }

    // Prueba que valida que el endpoint [POST /api/productos] retorne Bad Request si el usuario adjunta
    // un Producto no valido (que no cumple con las restricciones de datos implementadas en la clase
    // Producto) en el body de la solicitud. Espera como respuesta un status Bad Request
    // Se valida el correcto funcionamiento de la anotacion @Valid
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void guardarProductoNoValidoLanzaErrorTest() throws Exception{
        producto.setPrecio(-1.0); // Se asigna un numero negativo al precio (que no esta permitido)

        mockMvc.perform(post("/api/productos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(producto)))
            .andExpect(status().isBadRequest()); // Se espera un status Bad Request

    }

    // Prueba que valida que el endpoint [PUT /api/productos] retorne Bad Request si el usuario adjunta
    // un Producto no valido (que no cumple con las restricciones de datos implementadas en la clase
    // Producto) en el body de la solicitud. Espera como respuesta un status Bad Request
    // Se valida el correcto funcionamiento de la anotacion @Valid
    @Test
    @WithMockUser(authorities = {"ADMIN"})
    public void modificarProductoNoValidoLanzaErrorTest() throws Exception{
        producto.setNombre(""); // Se asigna un nombre en blanco al precio (que no esta permitido)

        mockMvc.perform(put("/api/productos/{id}", Long.valueOf(1))
            .contentType(MediaType.APPLICATION_JSON)
            .content(new ObjectMapper().writeValueAsString(producto)))
            .andExpect(status().isBadRequest()); // Se espera un status Bad Request

    }

}
