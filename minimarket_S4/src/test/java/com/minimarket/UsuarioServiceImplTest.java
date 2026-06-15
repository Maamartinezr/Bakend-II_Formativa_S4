package com.minimarket;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void findAllRetornaUsuariosRegistrados() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("EMPLEADO")));
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<Usuario> resultado = usuarioService.findAll();

        assertEquals(1, resultado.size());
        assertEquals("maria.vendedora", resultado.get(0).getUsername());
        verify(usuarioRepository).findAll();
    }

    @Test
    void findByUsernameRetornaUsuarioEncontrado() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("EMPLEADO")));
        when(usuarioRepository.findByUsername("maria.vendedora")).thenReturn(Optional.of(usuario));

        Optional<Usuario> resultado = usuarioService.findByUsername("maria.vendedora");

        assertTrue(resultado.isPresent());
        assertEquals("Maria", resultado.get().getNombre());
        verify(usuarioRepository).findByUsername("maria.vendedora");
    }

    @Test
    void savePersisteUsuarioConRepositorioMockeado() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("ADMIN")));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario resultado = usuarioService.save(usuario);

        assertEquals("maria.gonzalez@minimarket.cl", resultado.getEmail());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deleteByIdEliminaUsuarioPorIdentificador() {
        usuarioService.deleteById(1L);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void tieneDatosObligatoriosRetornaTrueCuandoUsuarioEstaCompleto() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("EMPLEADO")));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        boolean resultado = usuarioService.tieneDatosObligatorios(1L);

        assertTrue(resultado);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void tieneDatosObligatoriosRetornaFalseCuandoFaltaDireccion() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("EMPLEADO")));
        usuario.setDireccion(" ");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        boolean resultado = usuarioService.tieneDatosObligatorios(1L);

        assertFalse(resultado);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void tieneDatosObligatoriosRetornaFalseCuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        boolean resultado = usuarioService.tieneDatosObligatorios(99L);

        assertFalse(resultado);
        verify(usuarioRepository).findById(99L);
    }

    @Test
    void tieneRolValidoRetornaTrueCuandoUsuarioTieneRolPermitido() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("ADMIN")));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        boolean resultado = usuarioService.tieneRolValido(1L, Set.of("ADMIN", "EMPLEADO"));

        assertTrue(resultado);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void tieneRolValidoRetornaFalseCuandoUsuarioNoPuedeRegistrarVentas() {
        Usuario usuario = crearUsuarioCompleto(Set.of(new Rol("CLIENTE")));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        boolean resultado = usuarioService.tieneRolValido(1L, Set.of("ADMIN", "EMPLEADO"));

        assertFalse(resultado);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    void tieneRolValidoRetornaFalseCuandoNoSeInformanRolesPermitidos() {
        boolean resultado = usuarioService.tieneRolValido(1L, Set.of());

        assertFalse(resultado);
        verifyNoInteractions(usuarioRepository);
    }

    private Usuario crearUsuarioCompleto(Set<Rol> roles) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("maria.vendedora");
        usuario.setNombre("Maria");
        usuario.setApellido("Gonzalez");
        usuario.setEmail("maria.gonzalez@minimarket.cl");
        usuario.setDireccion("Av. Providencia 123");
        usuario.setPassword("ClaveSegura123");
        usuario.setRoles(roles);
        return usuario;
    }
}
