package com.minimarket.minimarket.security.config;

import com.minimarket.minimarket.config.RolEnum;
import com.minimarket.minimarket.security.filter.JwtAuthenticationFilter;
import com.minimarket.minimarket.security.service.CustomUserDetailsService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:8081",
            "http://localhost:8080",
            "http://localhost"
        ));;
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable()) // Deshabilita CSRF con la nueva sintaxis
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll() // Permitir acceso público
                        .requestMatchers("/auth/login/**").permitAll() // Acceso publico a la pagina de login
                        .requestMatchers("/h2-console/**").permitAll() // Permitir acceso a consola H2
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll() // Todos pueden ver los productos o buscar por ID
                        .requestMatchers("/api/productos/**").hasAuthority(RolEnum.ADMIN.name()) // Solo administradores pueden crear, editar o eliminar productos
                        .requestMatchers("/api/usuarios/**").hasAuthority(RolEnum.ADMIN.name()) // Solo administradores pueden realizar operaciones CRUD sobre usuarios
                        .requestMatchers("/api/inventario/**").hasAuthority(RolEnum.CAJERO.name()) // Solo cajeros pueden realizar operaciones sobre el inventario
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**").permitAll() // Todos pueden ver las categorias de productos
                        .requestMatchers("/api/categorias/**").hasAuthority(RolEnum.CAJERO.name()) // Solo cajeros pueden crear, modificar o eliminar categorias
                        .requestMatchers("/api/ventas/**").hasAuthority(RolEnum.CAJERO.name()) //Solo el cajero puede gestionar ventas
                        .requestMatchers("/api/detalle-ventas/**").hasAuthority(RolEnum.CAJERO.name()) // Solo cajeros pueden ver o gestionar detalles de ventas
                        .requestMatchers("/api/carrito/**").hasAuthority(RolEnum.CLIENTE.name()) // Solo clientes pueden administrar el carrito
                        .anyRequest().authenticated() // Requiere autenticación para el resto
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)) //Permitir carga de consola H2
                .sessionManagement(sessionManager->
                        sessionManager
                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Definicion del entorno stateless
                //Se comenta el formulario de login para poder probar los endpoints privados de la aplicacion
                //Si no se desactiva, la aplicacion redirige las solicitudes a rutas privadas a la pagina de login
                //.formLogin(form -> form
                //        .defaultSuccessUrl("/public/hola", true) // Redirigir después del login
                //)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/public/hola")
                        .permitAll()
                );

        // Añadir filtro JWT
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // Registrar proveedor de autenticación DAO
        http.authenticationProvider(authenticationProvider());

        return http.build();   
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Configuración de encriptación de contraseñas
    }
}
