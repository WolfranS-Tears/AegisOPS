package com.aegisops.backend.dto;

import com.aegisops.backend.domain.Categoria;
import com.aegisops.backend.domain.Usuario;

public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record UsuarioDto(Long id, String nombre, String correo, String area) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNombre(), u.getCorreo(), u.getArea());
        }
    }

    public record CategoriaDto(Long id, String nombre, String descripcion) {
        public static CategoriaDto de(Categoria c) {
            return new CategoriaDto(c.getId(), c.getNombre(), c.getDescripcion());
        }
    }
}
