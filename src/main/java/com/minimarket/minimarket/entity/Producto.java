package com.minimarket.minimarket.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Debe ingresar un nombre para el producto")
    @NotBlank(message = "El nombre del producto no puede ser un texto en blanco")
    @Column(nullable = false)
    private String nombre;

    @NotNull(message = "Debe ingresar el precio del producto")
    @Positive(message = "El precio del producto debe ser un numero positivo")
    @Column(nullable = false)
    private Double precio;

    @NotNull(message = "Debe ingresar el stock del producto")
    @PositiveOrZero(message = "El stock del producto debe ser igual o mayor a 0")
    @Column(nullable = false)
    private Integer stock;

    @NotNull(message = "Se debe asginar una categoria al Producto")
    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
