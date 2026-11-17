package com.aegisops.backend.service;

import com.aegisops.backend.dto.CatalogoDtos.CategoriaDto;
import com.aegisops.backend.dto.CatalogoDtos.UsuarioDto;
import com.aegisops.backend.exception.Excepciones;
import com.aegisops.backend.repository.CategoriaRepository;
import com.aegisops.backend.repository.UsuarioRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;

    public CatalogoService(UsuarioRepository usuarios, CategoriaRepository categorias) {
        this.usuarios = usuarios;
        this.categorias = categorias;
    }

    public List<UsuarioDto> usuarios() {
        return usuarios.findAll(Sort.by("nombre")).stream().map(UsuarioDto::de).toList();
    }

    public UsuarioDto usuario(Long id) {
        return usuarios.findById(id).map(UsuarioDto::de)
                .orElseThrow(() -> new Excepciones.NoEncontrado("Usuario", id));
    }

    public UsuarioDto usuarioPorCorreo(String correo) {
        return usuarios.findByCorreoIgnoreCase(correo.trim()).map(UsuarioDto::de)
                .orElseThrow(() -> new Excepciones.NoEncontrado("Usuario", correo));
    }

    public List<CategoriaDto> categorias() {
        return categorias.findAll(Sort.by("id")).stream().map(CategoriaDto::de).toList();
    }
}
