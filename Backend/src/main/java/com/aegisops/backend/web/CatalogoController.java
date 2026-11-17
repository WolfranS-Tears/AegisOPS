package com.aegisops.backend.web;

import com.aegisops.backend.dto.CatalogoDtos.CategoriaDto;
import com.aegisops.backend.dto.CatalogoDtos.UsuarioDto;
import com.aegisops.backend.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogos", description = "Usuarios y categorías")
public class CatalogoController {

    private final CatalogoService service;

    public CatalogoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/usuarios")
    public List<UsuarioDto> usuarios() {
        return service.usuarios();
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioDto usuario(@PathVariable Long id) {
        return service.usuario(id);
    }

    @Operation(summary = "Busca un usuario por correo (lo usa el inicio de sesión de demostración)")
    @GetMapping("/usuarios/por-correo")
    public UsuarioDto usuarioPorCorreo(@RequestParam String correo) {
        return service.usuarioPorCorreo(correo);
    }

    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() {
        return service.categorias();
    }
}
