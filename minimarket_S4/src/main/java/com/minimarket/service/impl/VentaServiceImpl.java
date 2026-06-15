package com.minimarket.service.impl;

import com.minimarket.entity.DetalleVenta;
import com.minimarket.entity.Producto;
import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import com.minimarket.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class VentaServiceImpl implements VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<Venta> findAll() {
        return ventaRepository.findAll();
    }

    @Override
    public Venta findById(Long id) {
        return ventaRepository.findById(id).orElse(null);
    }

    @Override
    public Venta save(Venta venta) {
        return ventaRepository.save(venta);
    }

    @Override
    public List<Venta> findByUsuarioId(Long usuarioId) {
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    @Override
    public Venta registrarVenta(Venta venta) {
        validarVenta(venta);
        venta.setTotal(calcularTotal(venta));
        return ventaRepository.save(venta);
    }

    @Override
    public boolean tieneStockSuficiente(Venta venta) {
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            return false;
        }

        return venta.getDetalles().stream().allMatch(detalle -> {
            Producto producto = obtenerProducto(detalle);
            return producto != null
                    && detalle.getCantidad() != null
                    && detalle.getCantidad() > 0
                    && producto.getStock() != null
                    && producto.getStock() >= detalle.getCantidad();
        });
    }

    @Override
    public double calcularTotal(Venta venta) {
        if (venta == null || venta.getDetalles() == null) {
            return 0.0;
        }

        return venta.getDetalles().stream()
                .mapToDouble(detalle -> {
                    Producto producto = obtenerProducto(detalle);
                    if (producto == null || producto.getPrecio() == null || detalle.getCantidad() == null) {
                        return 0.0;
                    }
                    return producto.getPrecio() * detalle.getCantidad();
                })
                .sum();
    }

    private void validarVenta(Venta venta) {
        if (venta == null || venta.getUsuario() == null || venta.getUsuario().getId() == null) {
            throw new IllegalArgumentException("La venta debe estar vinculada a un usuario valido");
        }

        Usuario usuario = usuarioRepository.findById(venta.getUsuario().getId())
                .orElseThrow(() -> new IllegalArgumentException("El usuario asociado a la venta no existe"));

        if (!usuarioTieneDatosCompletos(usuario)) {
            throw new IllegalArgumentException("El usuario debe tener nombre, apellido, email y direccion");
        }

        if (!usuarioTieneRolPermitido(usuario)) {
            throw new IllegalArgumentException("El usuario no tiene un rol autorizado para registrar ventas");
        }

        if (!tieneStockSuficiente(venta)) {
            throw new IllegalArgumentException("No existe stock suficiente para registrar la venta");
        }
    }

    private Producto obtenerProducto(DetalleVenta detalle) {
        if (detalle == null || detalle.getProducto() == null) {
            return null;
        }

        Long productoId = detalle.getProducto().getId();
        if (productoId == null) {
            return detalle.getProducto();
        }

        return productoRepository.findById(productoId).orElse(detalle.getProducto());
    }

    private boolean usuarioTieneDatosCompletos(Usuario usuario) {
        return noEstaVacio(usuario.getNombre())
                && noEstaVacio(usuario.getApellido())
                && noEstaVacio(usuario.getEmail())
                && noEstaVacio(usuario.getDireccion());
    }

    private boolean usuarioTieneRolPermitido(Usuario usuario) {
        Set<String> rolesPermitidos = Set.of("ADMIN", "EMPLEADO", "VENDEDOR");
        return usuario.getRoles() != null
                && usuario.getRoles().stream().anyMatch(rol -> rolesPermitidos.contains(rol.getNombre()));
    }

    private boolean noEstaVacio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}
