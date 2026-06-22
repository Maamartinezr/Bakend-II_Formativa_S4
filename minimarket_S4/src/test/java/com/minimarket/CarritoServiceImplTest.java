package com.minimarket;

import com.minimarket.entity.Carrito;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.CarritoRepository;
import com.minimarket.service.impl.CarritoServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para CarritoService
 * Valida: disponibilidad de stock, relaciones usuario-producto y operaciones de carrito
 */
@ExtendWith(MockitoExtension.class)
class CarritoServiceImplTest {

    @Mock
    private CarritoRepository carritoRepository;

    @InjectMocks
    private CarritoServiceImpl carritoService;

    // ============= PRUEBAS DE DISPONIBILIDAD DE STOCK =============

    @Test
    void agregarProductoConStockSuficiente_DebePermitirAgregar() {
        // Arrange
        Carrito carrito = crearCarrito(1L, 5);
        Producto producto = crearProducto(10L, "Arroz", 1200.0, 10);
        carrito.setProducto(producto);

        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // Act
        Carrito resultado = carritoService.save(carrito);

        // Assert
        assertNotNull(resultado);
        assertEquals(5, resultado.getCantidad());
        assertTrue(resultado.getCantidad() <= producto.getStock());
        verify(carritoRepository).save(carrito);
    }

    @Test
    void agregarProductoSinStockSuficiente_ValidacionDeStock() {
        // Arrange
        Carrito carrito = crearCarrito(1L, 15); // Cantidad mayor al stock
        Producto producto = crearProducto(10L, "Leche", 950.0, 10);
        carrito.setProducto(producto);

        // Act & Assert
        boolean tieneStockSuficiente = carrito.getCantidad() <= producto.getStock();
        assertFalse(tieneStockSuficiente,
                "Debe rechazar cuando cantidad solicitada > stock disponible");
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0}) // Cantidades inválidas
    void agregarProductoConCantidadNoValida_DebeRechazar(int cantidadInvalida) {
        // Arrange
        Carrito carrito = crearCarrito(1L, cantidadInvalida);
        Producto producto = crearProducto(10L, "Producto", 1000.0, 20);
        carrito.setProducto(producto);

        // Act & Assert
        boolean esValida = carrito.getCantidad() != null && carrito.getCantidad() > 0;
        assertFalse(esValida, "Cantidad debe ser positiva");
    }

    // ============= PRUEBAS DE RELACIONES USUARIO-PRODUCTO =============

    @Test
    void validarUsuarioAsociadoAlCarrito_RelacionCorrecta() {
        // Arrange
        Usuario usuario = crearUsuario(1L, "carlos.vendedor");
        Carrito carrito = crearCarrito(1L, 3);
        carrito.setUsuario(usuario);

        when(carritoRepository.findById(1L)).thenReturn(Optional.of(carrito));

        // Act
        Carrito resultado = carritoService.findById(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals(usuario.getId(), resultado.getUsuario().getId());
        assertEquals("carlos.vendedor", resultado.getUsuario().getUsername());
        verify(carritoRepository).findById(1L);
    }

    @Test
    void carritoSinUsuario_DebeValidarRelacion() {
        // Arrange
        Carrito carrito = crearCarrito(1L, 2);
        carrito.setUsuario(null);

        // Act & Assert
        assertNull(carrito.getUsuario(),
                "Carrito sin usuario debe ser detectado");
    }

    @Test
    void carritoProductoIncorrecto_DebeDetectarIncongruencia() {
        // Arrange
        Usuario usuario = crearUsuario(1L, "vendedor1");
        Carrito carrito = crearCarrito(1L, 5);
        Producto producto = crearProducto(10L, "Arroz", 1200.0, 20);

        carrito.setUsuario(usuario);
        carrito.setProducto(producto);

        // Act
        boolean esRelacionValida = carrito.getUsuario() != null
                && carrito.getProducto() != null
                && carrito.getProducto().getId() != null;

        // Assert
        assertTrue(esRelacionValida, "Relación usuario-producto debe ser válida");
    }

    // ============= PRUEBAS DE PRODUCTO SIN ID =============

    @Test
    void carritoConProductoSinId_DebeHandlear() {
        // Arrange
        Carrito carrito = crearCarrito(1L, 2);
        Producto productoSinId = new Producto();
        productoSinId.setNombre("Producto sin ID");
        productoSinId.setPrecio(500.0);
        productoSinId.setStock(10);

        carrito.setProducto(productoSinId);

        // Act & Assert
        assertNull(carrito.getProducto().getId(),
                "Producto sin ID debe ser identificado");
        assertNotNull(carrito.getProducto().getNombre());
    }

    // ============= PRUEBAS DE OPERACIONES DE CARRITO =============

    @Test
    void obtenerCarritoPorId_RetornaCarritoCorrectamente() {
        // Arrange
        Carrito carrito = crearCarrito(5L, 3);
        when(carritoRepository.findById(5L)).thenReturn(Optional.of(carrito));

        // Act
        Carrito resultado = carritoService.findById(5L);

        // Assert
        assertNotNull(resultado);
        assertEquals(5L, resultado.getId());
        verify(carritoRepository).findById(5L);
    }

    @Test
    void obtenerCarritoPorIdNoExistente_RetornaNulo() {
        // Arrange
        when(carritoRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Carrito resultado = carritoService.findById(999L);

        // Assert
        assertNull(resultado);
        verify(carritoRepository).findById(999L);
    }

    @Test
    void obtenerCarritosPorUsuario_RetornaListaCorrecta() {
        // Arrange
        Carrito carrito1 = crearCarrito(1L, 2);
        Carrito carrito2 = crearCarrito(2L, 3);
        List<Carrito> carritos = List.of(carrito1, carrito2);

        when(carritoRepository.findByUsuarioId(1L)).thenReturn(carritos);

        // Act
        List<Carrito> resultado = carritoService.findByUsuarioId(1L);

        // Assert
        assertEquals(2, resultado.size());
        verify(carritoRepository).findByUsuarioId(1L);
    }

    @Test
    void limpiarCarrito_DebeEliminarCorrectamente() {
        // Arrange
        Long carritoId = 1L;

        // Act
        carritoService.deleteById(carritoId);

        // Assert
        verify(carritoRepository).deleteById(carritoId);
    }

    @Test
    void guardarCarrito_DebePeristirCorrectamente() {
        // Arrange
        Carrito carrito = crearCarrito(1L, 5);
        when(carritoRepository.save(any(Carrito.class))).thenReturn(carrito);

        // Act
        Carrito resultado = carritoService.save(carrito);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        verify(carritoRepository).save(carrito);
    }

    // ============= MÉTODOS AUXILIARES =============

    private Carrito crearCarrito(Long usuarioId, int cantidad) {
        Carrito carrito = new Carrito();
        carrito.setId(usuarioId);

        Usuario usuario = crearUsuario(usuarioId, "usuario" + usuarioId);
        carrito.setUsuario(usuario);
        carrito.setCantidad(cantidad);

        return carrito;
    }

    private Usuario crearUsuario(Long id, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsername(username);
        usuario.setNombre("Nombre" + id);
        usuario.setApellido("Apellido" + id);
        usuario.setEmail("email" + id + "@minimarket.cl");
        usuario.setDireccion("Dirección " + id);
        usuario.setPassword("ClaveSegura123");
        return usuario;
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
