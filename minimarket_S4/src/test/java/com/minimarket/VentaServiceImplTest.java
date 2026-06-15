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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void calcularTotalSumaPrecioPorCantidadDeCadaProducto() {
        Venta venta = crearVentaConDetalles(2, 3);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(crearProducto(10L, "Arroz", 1200.0, 5)));
        when(productoRepository.findById(11L)).thenReturn(Optional.of(crearProducto(11L, "Leche", 950.0, 8)));

        double total = ventaService.calcularTotal(venta);

        assertEquals(5250.0, total);
    }

    private Venta crearVentaConDetalles(int cantidadArroz, int cantidadLeche) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        DetalleVenta arroz = crearDetalle(10L, cantidadArroz);
        DetalleVenta leche = crearDetalle(11L, cantidadLeche);
        arroz.setVenta(venta);
        leche.setVenta(venta);
        venta.setDetalles(List.of(arroz, leche));
        return venta;
    }

    private DetalleVenta crearDetalle(Long productoId, int cantidad) {
        Producto producto = new Producto();
        producto.setId(productoId);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
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
