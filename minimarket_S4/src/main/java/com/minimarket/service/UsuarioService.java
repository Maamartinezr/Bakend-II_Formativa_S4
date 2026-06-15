package com.minimarket.service;

import com.minimarket.entity.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsuarioService {
    List<Usuario> findAll();
    Optional<Usuario> findById(Long id);
    Optional<Usuario> findByUsername(String username);
    Usuario save(Usuario usuario);
    void deleteById(Long id);
    boolean tieneDatosObligatorios(Long id);
    boolean tieneRolValido(Long id, Set<String> rolesPermitidos);
}
