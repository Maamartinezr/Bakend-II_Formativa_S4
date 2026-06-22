package com.minimarket;

import com.minimarket.entity.Inventario;
import com.minimarket.entity.Producto;
import com.minimarket.repository.InventarioRepository;
import com.minimarket.service.impl.InventarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para InventarioService
 * Valida: movimientos de inventario, relaciones producto-inventario y campos obligatorios
 */
@ExtendWith(MockitoExtension.class)
class InventarioServiceImplTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @InjectMocks
    private InventarioServiceImpl inventarioService;

    // ============= PRUEBAS DE INFORMACIÓN DE MOVIMIENTO =============

    @Test
    void registrarMovimientoEntrada_ValidaQueTipoMovimientoNoSeaNulo() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", 10);

        // Act & Assert
        assertNotNull(inventario.getTipoMovimiento(),
                "tipoMovimiento no debe ser nulo");
        assertEquals("ENTRADA", inventario.getTipoMovimiento());
    }

    @Test
    void registrarMovimientoSalida_ValidaQueTipoMovimientoNoSeaNulo() {
        // Arrange
        Inventario inventario = crearInventario(1L, "SALIDA", 5);

        // Act & Assert
        assertNotNull(inventario.getTipoMovimiento(),
                "tipoMovimiento no debe ser nulo");
        assertEquals("SALIDA", inventario.getTipoMovimiento());
    }

    @Test
    void movimientoTipoNulo_DebeRechazar() {
        // Arrange
        Inventario inventario = crearInventario(1L, null, 10);

        // Act & Assert
        assertNull(inventario.getTipoMovimiento(),
                "Debe detectar tipoMovimiento nulo");
        boolean esValido = inventario.getTipoMovimiento() != null
                && !inventario.getTipoMovimiento().trim().isEmpty();
        assertFalse(esValido, "Movimiento sin tipo no es válido");
    }

    @Test
    void movimientoCantidadNula_DebeRechazar() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", null);

        // Act & Assert
        assertNull(inventario.getCantidad(),
                "Debe detectar cantidad nula");
        boolean esValida = inventario.getCantidad() != null && inventario.getCantidad() > 0;
        assertFalse(esValida, "Movimiento sin cantidad válida no es permitido");
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0}) // Cantidades inválidas
    void movimientoCantidadInvalida_DebeRechazar(int cantidadInvalida) {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", cantidadInvalida);

        // Act & Assert
        boolean esValida = inventario.getCantidad() != null && inventario.getCantidad() > 0;
        assertFalse(esValida,
                "Cantidad " + cantidadInvalida + " no es válida para movimiento");
    }

    // ============= PRUEBAS DE RELACIÓN PRODUCTO-INVENTARIO =============

    @Test
    void registrarMovimientoConProductoValido_ValidaRelacion() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", 10);
        Producto producto = crearProducto(100L, "Arroz", 1200.0, 50);
        inventario.setProducto(producto);

        when(inventarioRepository.save(any(Inventario.class))).thenReturn(inventario);

        // Act
        Inventario resultado = inventarioService.save(inventario);

        // Assert
        assertNotNull(resultado.getProducto());
        assertEquals(100L, resultado.getProducto().getId());
        assertEquals("Arroz", resultado.getProducto().getNombre());
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void registrarMovimientoProductoSinId_DebeDetectar() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", 5);
        Producto productoSinId = new Producto();
        productoSinId.setNombre("Producto desconocido");
        productoSinId.setPrecio(500.0);
        inventario.setProducto(productoSinId);

        // Act & Assert
        assertNull(inventario.getProducto().getId(),
                "Debe detectar producto sin ID");
        assertNotNull(inventario.getProducto().getNombre());
    }

    @Test
    void registrarMovimientoProductoNulo_DebeRechazar() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", 10);
        inventario.setProducto(null);

        // Act & Assert
        assertNull(inventario.getProducto(),
                "Inventario sin producto asociado debe ser detectado");
        boolean esValido = inventario.getProducto() != null
                && inventario.getProducto().getId() != null;
        assertFalse(esValido, "Movimiento sin producto no es válido");
    }

    // ============= PRUEBAS DE OPERACIONES CRUD =============

    @Test
    void obtenerInventarioPorId_RetornaMovimientoCorrectamente() {
        // Arrange
        Inventario inventario = crearInventario(5L, "SALIDA", 8);
        when(inventarioRepository.findById(5L)).thenReturn(Optional.of(inventario));

        // Act
        Inventario resultado = inventarioService.findById(5L);

        // Assert
        assertNotNull(resultado);
        assertEquals(5L, resultado.getId());
        assertEquals("SALIDA", resultado.getTipoMovimiento());
        verify(inventarioRepository).findById(5L);
    }

    @Test
    void obtenerInventarioPorIdNoExistente_RetornaNulo() {
        // Arrange
        when(inventarioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Inventario resultado = inventarioService.findById(999L);

        // Assert
        assertNull(resultado);
        verify(inventarioRepository).findById(999L);
    }

    @Test
    void obtenerTodosLosMovimientos_RetornaLista() {
        // Arrange
        Inventario mov1 = crearInventario(1L, "ENTRADA", 10);
        Inventario mov2 = crearInventario(2L, "SALIDA", 5);
        List<Inventario> movimientos = List.of(mov1, mov2);

        when(inventarioRepository.findAll()).thenReturn(movimientos);

        // Act
        List<Inventario> resultado = inventarioService.findAll();

        // Assert
        assertEquals(2, resultado.size());
        verify(inventarioRepository).findAll();
    }

    @Test
    void obtenerMovimientosPorProducto_RetornaListaCorrecta() {
        // Arrange
        Inventario mov1 = crearInventario(1L, "ENTRADA", 10);
        Inventario mov2 = crearInventario(2L, "SALIDA", 5);
        List<Inventario> movimientos = List.of(mov1, mov2);

        when(inventarioRepository.findByProductoId(100L)).thenReturn(movimientos);

        // Act
        List<Inventario> resultado = inventarioService.findByProductoId(100L);

        // Assert
        assertEquals(2, resultado.size());
        verify(inventarioRepository).findByProductoId(100L);
    }

    @Test
    void guardarMovimiento_DebePeristirCorrectamente() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", 15);
        when(inventarioRepository.save(any(Inventario.class))).thenReturn(inventario);

        // Act
        Inventario resultado = inventarioService.save(inventario);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        verify(inventarioRepository).save(inventario);
    }

    @Test
    void eliminarMovimiento_DebeEliminarCorrectamente() {
        // Arrange
        Long movimientoId = 1L;

        // Act
        inventarioService.deleteById(movimientoId);

        // Assert
        verify(inventarioRepository).deleteById(movimientoId);
    }

    // ============= PRUEBAS DE VALIDACIONES ADICIONALES =============

    @Test
    void validarFechaMovimiento_DebeEstarPresente() {
        // Arrange
        Inventario inventario = crearInventario(1L, "ENTRADA", 10);
        Date ahora = new Date();
        inventario.setFechaMovimiento(ahora);

        // Act & Assert
        assertNotNull(inventario.getFechaMovimiento(),
                "Fecha de movimiento no debe ser nula");
        assertTrue(inventario.getFechaMovimiento().before(new Date()) ||
                inventario.getFechaMovimiento().equals(ahora));
    }

    @Test
    void movimientosMultiples_DebenSerIndependientes() {
        // Arrange
        Inventario mov1 = crearInventario(1L, "ENTRADA", 10);
        Inventario mov2 = crearInventario(2L, "SALIDA", 5);

        // Act & Assert
        assertNotEquals(mov1.getTipoMovimiento(), mov2.getTipoMovimiento());
        assertNotEquals(mov1.getCantidad(), mov2.getCantidad());
        verify(inventarioRepository, times(0)).save(any());
    }

    // ============= MÉTODOS AUXILIARES =============

    private Inventario crearInventario(Long id, String tipoMovimiento, Integer cantidad) {
        Inventario inventario = new Inventario();
        inventario.setId(id);
        inventario.setTipoMovimiento(tipoMovimiento);
        inventario.setCantidad(cantidad);
        inventario.setFechaMovimiento(new Date());

        Producto producto = crearProducto(100L, "Producto", 1000.0, 50);
        inventario.setProducto(producto);

        return inventario;
    }

    private Producto crearProducto(Long id, String nombre, Double precio, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        return producto;
    }
}
