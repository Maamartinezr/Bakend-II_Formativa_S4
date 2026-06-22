package com.minimarket;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import com.minimarket.service.impl.VentaServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para VentaService - Semana 5
 * Incluye mejoras del feedback del profesor:
 * - Validaciones de precios nulos
 * - Campos faltantes en cálculo total
 * - Cantidades negativas y cero
 * - Productos sin ID
 * - Pruebas parametrizadas
 * - Precisión monetaria
 */
@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    // ============= PRUEBAS BÁSICAS DE REGISTRO =============

    @Test
    void registrarVentaGuardaCuandoUsuarioEsValidoYExisteStockSuficiente() {
        Venta venta = crearVentaConDetalles(2, 3);
        Producto arroz = crearProducto(10L, "Arroz", 1200.0, 10);
        Producto leche = crearProducto(11L, "Leche", 950.0, 8);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearUsuarioValido("EMPLEADO")));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(arroz));
        when(productoRepository.findById(11L)).thenReturn(Optional.of(leche));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Venta resultado = ventaService.registrarVenta(venta);

        assertEquals(5250.0, resultado.getTotal());
        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertEquals(5250.0, ventaCaptor.getValue().getTotal());
    }

    @Test
    void registrarVentaRechazaVentaCuandoNoHayStockSuficiente() {
        Venta venta = crearVentaConDetalles(6, 1);
        Producto arroz = crearProducto(10L, "Arroz", 1200.0, 5);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearUsuarioValido("EMPLEADO")));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(arroz));

        assertThrows(IllegalArgumentException.class, () -> ventaService.registrarVenta(venta));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaRechazaUsuarioConDatosIncompletos() {
        Venta venta = crearVentaConDetalles(1, 1);
        Usuario usuario = crearUsuarioValido("ADMIN");
        usuario.setEmail("");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class, () -> ventaService.registrarVenta(venta));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void registrarVentaRechazaUsuarioSinRolAutorizado() {
        Venta venta = crearVentaConDetalles(1, 1);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearUsuarioValido("CLIENTE")));

        assertThrows(IllegalArgumentException.class, () -> ventaService.registrarVenta(venta));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    // ============= PRUEBAS DE VALIDACIÓN DE PRECIOS NULOS =============

    @Test
    void calcularTotalConPrecioNulo_DebeValidar() {
        // Arrange
        Venta venta = new Venta();
        Usuario usuario = crearUsuarioValido("EMPLEADO");
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        Producto productoSinPrecio = new Producto();
        productoSinPrecio.setId(10L);
        productoSinPrecio.setNombre("Producto");
        productoSinPrecio.setPrecio(null); // Precio nulo

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(productoSinPrecio);
        detalle.setCantidad(5);
        detalle.setVenta(venta);

        venta.setDetalles(List.of(detalle));

        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoSinPrecio));

        // Act
        double total = ventaService.calcularTotal(venta);

        // Assert
        assertEquals(0.0, total, "Detalle con precio nulo debe sumar 0");
    }

    @Test
    void calcularTotalConCamposFaltantes_DebeValidar() {
        // Arrange
        Venta venta = new Venta();
        venta.setDetalles(null); // Sin detalles

        // Act
        double total = ventaService.calcularTotal(venta);

        // Assert
        assertEquals(0.0, total, "Venta sin detalles debe retornar 0");
    }

    @Test
    void calcularTotalConDetallesVacio_DebeRetornarCero() {
        // Arrange
        Venta venta = new Venta();
        venta.setDetalles(List.of());

        // Act
        double total = ventaService.calcularTotal(venta);

        // Assert
        assertEquals(0.0, total, "Venta con detalles vacío debe retornar 0");
    }

    // ============= PRUEBAS DE CANTIDADES INVÁLIDAS =============

    @Test
    void calcularTotalConCantidadNegativa_DebeRechazar() {
        // Arrange
        Venta venta = crearVentaConDetalles(-5, 1); // Cantidad negativa
        Producto arroz = crearProducto(10L, "Arroz", 1200.0, 20);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearUsuarioValido("EMPLEADO")));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(arroz));

        // Act & Assert
        boolean tieneStock = ventaService.tieneStockSuficiente(venta);
        assertFalse(tieneStock, "Cantidad negativa no debe pasar validación de stock");
    }

    @Test
    void calcularTotalConCantidadCero_DebeRechazar() {
        // Arrange
        Venta venta = crearVentaConDetalles(0, 1); // Cantidad cero
        Producto arroz = crearProducto(10L, "Arroz", 1200.0, 20);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(arroz));

        // Act & Assert
        boolean tieneStock = ventaService.tieneStockSuficiente(venta);
        assertFalse(tieneStock, "Cantidad cero no debe pasar validación de stock");
    }

    @Test
    void calcularTotalConCantidadNula_DebeRechazar() {
        // Arrange
        Venta venta = new Venta();
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(crearProducto(10L, "Arroz", 1200.0, 20));
        detalle.setCantidad(null); // Cantidad nula
        detalle.setVenta(venta);

        venta.setDetalles(List.of(detalle));

        when(productoRepository.findById(10L)).thenReturn(Optional.of(crearProducto(10L, "Arroz", 1200.0, 20)));

        // Act & Assert
        boolean tieneStock = ventaService.tieneStockSuficiente(venta);
        assertFalse(tieneStock, "Cantidad nula no debe pasar validación");
    }

    // ============= PRUEBAS DE PRODUCTO NO ENCONTRADO =============

    @Test
    void detalleVentaProductoNoEncontrado_DebeHandlear() {
        // Arrange
        Venta venta = crearVentaConDetalles(2, 1);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(crearUsuarioValido("EMPLEADO")));
        when(productoRepository.findById(10L)).thenReturn(Optional.empty()); // Producto no encontrado

        // Act & Assert
        boolean tieneStock = ventaService.tieneStockSuficiente(venta);
        assertFalse(tieneStock, "Producto no encontrado debe fallar en validación");
    }

    @Test
    void obtenerProductoConIdNulo_DebeHandlear() {
        // Arrange
        Venta venta = new Venta();
        Usuario usuario = crearUsuarioValido("EMPLEADO");
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        Producto productoSinId = new Producto();
        productoSinId.setId(null);
        productoSinId.setNombre("Producto sin ID");
        productoSinId.setPrecio(1000.0);
        productoSinId.setStock(10);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(productoSinId);
        detalle.setCantidad(2);
        detalle.setVenta(venta);

        venta.setDetalles(List.of(detalle));

        // Act
        double total = ventaService.calcularTotal(venta);

        // Assert
        assertEquals(2000.0, total, "Debe usar el producto incrustado cuando no hay ID");
    }

    // ============= PRUEBAS PARAMETRIZADAS MÚLTIPLES COMBINACIONES =============

    @ParameterizedTest
    @CsvSource({
            "1200.0, 2, 2400.0",      // Caso normal
            "950.5, 3, 2851.5",        // Precio decimal
            "100.0, 1, 100.0",         // Una unidad
            "0.99, 100, 99.0"          // Muchas unidades
    })
    void calcularTotalMultiplesCombinaciones(Double precio, Integer cantidad, Double esperado) {
        // Arrange
        Venta venta = crearVentaConDetalles(cantidad, 1);
        Producto producto = crearProducto(10L, "Producto", precio, 100);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        // Act
        double total = ventaService.calcularTotal(venta);

        // Assert
        assertEquals(esperado, total, 0.01, "Cálculo debe ser preciso");
    }

    // ============= PRUEBAS DE PRECISIÓN MONETARIA =============

    @Test
    void calcularTotalSinErroresRedondeo_DebeMantenerlaPrecision() {
        // Arrange - Simulación de cálculo con múltiples detalles
        Venta venta = new Venta();
        Usuario usuario = crearUsuarioValido("EMPLEADO");
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        DetalleVenta detalle1 = crearDetalle(10L, 1234.567, 2);
        DetalleVenta detalle2 = crearDetalle(11L, 890.123, 3);

        detalle1.setVenta(venta);
        detalle2.setVenta(venta);
        venta.setDetalles(List.of(detalle1, detalle2));

        Producto prod1 = crearProducto(10L, "Prod1", 1234.567, 10);
        Producto prod2 = crearProducto(11L, "Prod2", 890.123, 10);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(prod1));
        when(productoRepository.findById(11L)).thenReturn(Optional.of(prod2));

        // Act
        double total = ventaService.calcularTotal(venta);

        // Assert
        double esperado = (1234.567 * 2) + (890.123 * 3);
        assertEquals(esperado, total, 0.01, "Cálculo monetario debe mantener precisión");
    }

    // ============= PRUEBAS DE STOCK SUFICIENTE =============

    @Test
    void tieneStockSuficienteSimulaConsultaDeStockDeProductos() {
        Venta venta = crearVentaConDetalles(2, 1);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(crearProducto(10L, "Arroz", 1200.0, 5)));
        when(productoRepository.findById(11L)).thenReturn(Optional.of(crearProducto(11L, "Leche", 950.0, 8)));

        boolean resultado = ventaService.tieneStockSuficiente(venta);

        assertTrue(resultado);
        verify(productoRepository).findById(10L);
        verify(productoRepository).findById(11L);
    }

    @Test
    void tieneStockSuficienteConVentaNula_DebeRetornarFalso() {
        // Act & Assert
        boolean resultado = ventaService.tieneStockSuficiente(null);
        assertFalse(resultado, "Venta nula no tiene stock suficiente");
    }

    @Test
    void tieneStockSuficienteConDetallesVacio_DebeRetornarFalso() {
        // Arrange
        Venta venta = new Venta();
        venta.setDetalles(List.of());

        // Act & Assert
        boolean resultado = ventaService.tieneStockSuficiente(venta);
        assertFalse(resultado, "Venta sin detalles no tiene stock suficiente");
    }

    // ============= PRUEBAS DE MÉTODOS CRUD =============

    @Test
    void findAllRetornaVentas() {
        Venta venta1 = new Venta();
        Venta venta2 = new Venta();
        when(ventaRepository.findAll()).thenReturn(List.of(venta1, venta2));

        List<Venta> resultado = ventaService.findAll();

        assertEquals(2, resultado.size());
        verify(ventaRepository).findAll();
    }

    @Test
    void findByIdRetornaVentaEncontrada() {
        Venta venta = new Venta();
        venta.setId(1L);
        when(ventaRepository.findById(1L)).thenReturn(Optional.of(venta));

        Venta resultado = ventaService.findById(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    void findByUsuarioIdRetornaVentasDelUsuario() {
        Venta venta1 = new Venta();
        Venta venta2 = new Venta();
        when(ventaRepository.findByUsuarioId(1L)).thenReturn(List.of(venta1, venta2));

        List<Venta> resultado = ventaService.findByUsuarioId(1L);

        assertEquals(2, resultado.size());
        verify(ventaRepository).findByUsuarioId(1L);
    }

    // ============= MÉTODOS AUXILIARES =============

    private Venta crearVentaConDetalles(int cantidadArroz, int cantidadLeche) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        DetalleVenta arroz = crearDetalle(10L, 1200.0, cantidadArroz);
        DetalleVenta leche = crearDetalle(11L, 950.0, cantidadLeche);
        arroz.setVenta(venta);
        leche.setVenta(venta);
        venta.setDetalles(List.of(arroz, leche));
        return venta;
    }

    private DetalleVenta crearDetalle(Long productoId, Double precio, int cantidad) {
        Producto producto = new Producto();
        producto.setId(productoId);
        producto.setPrecio(precio);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecio(precio);
        return detalle;
    }

    private Producto crearProducto(Long id, String nombre, Double precio, Integer stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        return producto;
    }

    private Producto crearProducto(Long id, String nombre, Double precio) {
        return crearProducto(id, nombre, precio, 50);
    }

    private Usuario crearUsuarioValido(String rol) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("vendedor");
        usuario.setNombre("Carlos");
        usuario.setApellido("Perez");
        usuario.setEmail("carlos.perez@minimarket.cl");
        usuario.setDireccion("Los Leones 456");
        usuario.setPassword("ClaveSegura123");
        usuario.setRoles(Set.of(new Rol(rol)));
        return usuario;
    }
}
