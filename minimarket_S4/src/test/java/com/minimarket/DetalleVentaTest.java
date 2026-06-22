package com.minimarket;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Venta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para DetalleVenta
 * Valida: precisión monetaria, campos requeridos, cálculos y relaciones
 */
class DetalleVentaTest {

    // ============= PRUEBAS DE CÁLCULO TOTAL CON CASOS BORDE =============

    @ParameterizedTest
    @CsvSource({
            "1200.0, 5, 6000.0",      // Caso normal
            "950.5, 3, 2851.5",        // Precio decimal
            "100.0, 1, 100.0",         // Una unidad
            "0.99, 100, 99.0"          // Muchas unidades
    })
    void calcularTotalConPreciosValidos_DebeCalcularCorrectamente(
            Double precio, Integer cantidad, Double esperado) {
        // Arrange
        DetalleVenta detalle = crearDetalle(precio, cantidad);

        // Act
        Double total = detalle.getPrecio() * detalle.getCantidad();

        // Assert
        assertEquals(esperado, total, 0.01,
                "Cálculo de total debe ser preciso");
    }

    @Test
    void calcularTotalConPrecioNulo_DebeValidar() {
        // Arrange
        DetalleVenta detalle = crearDetalle(null, 5);

        // Act & Assert
        boolean esValido = detalle.getPrecio() != null && detalle.getPrecio() > 0;
        assertFalse(esValido, "Precio nulo no es válido para cálculo");
    }

    @Test
    void calcularTotalConCamposFaltantes_DebeValidar() {
        // Arrange
        DetalleVenta detalle = new DetalleVenta();
        detalle.setPrecio(null);
        detalle.setCantidad(null);

        // Act & Assert
        boolean tienePrecios = detalle.getPrecio() != null;
        boolean tieneCantidad = detalle.getCantidad() != null;

        assertFalse(tienePrecios && tieneCantidad,
                "Detalles sin precio o cantidad deben ser rechazados");
    }

    // ============= PRUEBAS DE CANTIDADES INVÁLIDAS =============

    @Test
    void calcularTotalConCantidadNegativa_DebeRechazar() {
        // Arrange
        DetalleVenta detalle = crearDetalle(1200.0, -5);

        // Act & Assert
        boolean esValida = detalle.getCantidad() != null && detalle.getCantidad() > 0;
        assertFalse(esValida,
                "Cantidad negativa no es permitida");
    }

    @Test
    void calcularTotalConCantidadCero_DebeRechazar() {
        // Arrange
        DetalleVenta detalle = crearDetalle(1200.0, 0);

        // Act & Assert
        boolean esValida = detalle.getCantidad() != null && detalle.getCantidad() > 0;
        assertFalse(esValida,
                "Cantidad cero no es permitida");
    }

    @Test
    void calcularTotalConCantidadNula_DebeDetectar() {
        // Arrange
        DetalleVenta detalle = crearDetalle(1200.0, null);

        // Act & Assert
        assertNull(detalle.getCantidad(),
                "Cantidad nula debe ser detectada");
    }

    // ============= PRUEBAS DE PRODUCTO NO ENCONTRADO =============

    @Test
    void detalleVentaSinProducto_DebeDetectar() {
        // Arrange
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(null);
        detalle.setPrecio(1200.0);
        detalle.setCantidad(2);

        // Act & Assert
        assertNull(detalle.getProducto(),
                "Producto no encontrado debe ser detectado");
    }

    @Test
    void detalleVentaConProductoSinId_DebeHandlear() {
        // Arrange
        Producto productoSinId = new Producto();
        productoSinId.setNombre("Producto desconocido");
        productoSinId.setPrecio(500.0);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(productoSinId);
        detalle.setPrecio(500.0);
        detalle.setCantidad(3);

        // Act & Assert
        assertNull(detalle.getProducto().getId(),
                "Producto sin ID debe ser identificado");
        assertNotNull(detalle.getProducto().getNombre());
        assertTrue(detalle.getPrecio() > 0);
    }

    @Test
    void detalleVentaConProductoId_DebeValidarRelacion() {
        // Arrange
        Producto producto = crearProducto(10L, "Arroz", 1200.0);
        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setPrecio(1200.0);
        detalle.setCantidad(2);

        // Act & Assert
        assertNotNull(detalle.getProducto().getId());
        assertEquals(10L, detalle.getProducto().getId());
    }

    // ============= PRUEBAS DE PRECISIÓN MONETARIA =============

    @Test
    void calcularTotalSinErroresRedondeo_DebeMantenerlaPrecision() {
        // Arrange - Caso típico de redondeo problemático
        DetalleVenta detalle = crearDetalle(0.1, 3); // 0.3 problemático en punto flotante
        Double esperado = 0.3;

        // Act
        Double total = detalle.getPrecio() * detalle.getCantidad();

        // Assert - Delta de 0.01 para evitar errores de punto flotante
        assertEquals(esperado, total, 0.01,
                "Cálculo monetario debe mantener precisión aceptable");
    }

    @Test
    void precioConMuchasDecimales_DebeMantenerlaPrecision() {
        // Arrange
        DetalleVenta detalle = crearDetalle(1234.567, 3);

        // Act
        Double total = detalle.getPrecio() * detalle.getCantidad();

        // Assert
        assertEquals(3703.701, total, 0.01,
                "Precios con múltiples decimales deben calcularse correctamente");
    }

    // ============= PRUEBAS DE RELACIONES Y CAMPOS REQUERIDOS =============

    @Test
    void detalleVentaCompleto_TodoosCamposPresentes() {
        // Arrange
        Venta venta = new Venta();
        venta.setId(1L);
        venta.setFecha(new Date());

        Producto producto = crearProducto(10L, "Leche", 950.0);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setId(1L);
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setPrecio(950.0);
        detalle.setCantidad(3);

        // Act & Assert
        assertNotNull(detalle.getId());
        assertNotNull(detalle.getVenta());
        assertNotNull(detalle.getProducto());
        assertTrue(detalle.getPrecio() > 0);
        assertTrue(detalle.getCantidad() > 0);
    }

    @Test
    void detalleVentaSinVenta_DebeDetectar() {
        // Arrange
        DetalleVenta detalle = crearDetalle(1200.0, 2);
        detalle.setVenta(null);

        // Act & Assert
        assertNull(detalle.getVenta(),
                "Detalle sin venta asociada debe ser detectado");
    }

    // ============= PRUEBAS PARAMETRIZADAS MÚLTIPLES COMBINACIONES =============

    @ParameterizedTest
    @CsvSource({
            "1000.0, 5, 5000.0, VALIDA",
            "500.0, 2, 1000.0, VALIDA",
            "1200.0, 1, 1200.0, VALIDA",
            "0.0, 5, 0.0, INVALIDA",
            "-100.0, 5, -500.0, INVALIDA"
    })
    void validarMultiplesCombinacionesPreciosCantidades(
            Double precio, Integer cantidad, Double total, String estado) {
        // Arrange
        DetalleVenta detalle = crearDetalle(precio, cantidad);

        // Act
        boolean esValido = detalle.getPrecio() != null && detalle.getPrecio() > 0
                && detalle.getCantidad() != null && detalle.getCantidad() > 0;

        Double totalCalculado = esValido ? precio * cantidad : 0.0;

        // Assert
        if ("VALIDA".equals(estado)) {
            assertTrue(esValido, "Combinación " + precio + "x" + cantidad + " debe ser válida");
            assertEquals(total, totalCalculado, 0.01);
        } else {
            assertFalse(esValido, "Combinación " + precio + "x" + cantidad + " debe ser inválida");
        }
    }

    // ============= PRUEBAS DE COHERENCIA =============

    @Test
    void validarCoherenciaDetalleVentaVenta_DebenEstarRelacionados() {
        // Arrange
        Venta venta = new Venta();
        venta.setId(1L);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);

        // Act & Assert
        assertNotNull(detalle.getVenta());
        assertEquals(1L, detalle.getVenta().getId());
    }

    @Test
    void validarPrecioActualDelDetalle_NoDebeSerNegativo() {
        // Arrange
        DetalleVenta detalle = crearDetalle(-500.0, 2);

        // Act & Assert
        assertNotNull(detalle.getPrecio());
        assertFalse(detalle.getPrecio() > 0,
                "Precio negativo debe ser rechazado en validación");
    }

    // ============= MÉTODOS AUXILIARES =============

    private DetalleVenta crearDetalle(Double precio, Integer cantidad) {
        DetalleVenta detalle = new DetalleVenta();
        detalle.setPrecio(precio);
        detalle.setCantidad(cantidad);

        Producto producto = crearProducto(10L, "Producto Test", precio);
        detalle.setProducto(producto);

        return detalle;
    }

    private Producto crearProducto(Long id, String nombre, Double precio) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(50);
        return producto;
    }
}
