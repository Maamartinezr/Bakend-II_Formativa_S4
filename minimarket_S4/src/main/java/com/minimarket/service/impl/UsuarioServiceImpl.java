package com.minimarket.service.impl;

import com.minimarket.entity.Usuario;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    @Override
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    public boolean tieneDatosObligatorios(Long id) {
        return usuarioRepository.findById(id)
                .map(this::tieneDatosObligatorios)
                .orElse(false);
    }

    @Override
    public boolean tieneRolValido(Long id, Set<String> rolesPermitidos) {
        if (rolesPermitidos == null || rolesPermitidos.isEmpty()) {
            return false;
        }

        return usuarioRepository.findById(id)
                .filter(usuario -> usuario.getRoles() != null)
                .map(usuario -> usuario.getRoles().stream()
                        .anyMatch(rol -> rolesPermitidos.contains(rol.getNombre())))
                .orElse(false);
    }

    public boolean tieneDatosObligatorios(Usuario usuario) {
        return usuario != null
                && noEstaVacio(usuario.getNombre())
                && noEstaVacio(usuario.getApellido())
                && noEstaVacio(usuario.getEmail())
                && noEstaVacio(usuario.getDireccion());
    }

    private boolean noEstaVacio(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}
