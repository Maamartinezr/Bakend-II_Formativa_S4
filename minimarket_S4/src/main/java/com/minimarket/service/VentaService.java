package com.minimarket.service;

import com.minimarket.entity.Venta;

import java.util.List;

public interface VentaService {
    List<Venta> findAll();
    Venta findById(Long id);
    Venta save(Venta venta);
    List<Venta> findByUsuarioId(Long usuarioId);
    Venta registrarVenta(Venta venta);
    boolean tieneStockSuficiente(Venta venta);
    double calcularTotal(Venta venta);
}
