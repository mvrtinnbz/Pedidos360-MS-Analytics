package com.pedidos360.ms_analytics.recomendaciones;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class RecomendacionController {

    private final RecomendacionService service;

    public RecomendacionController(RecomendacionService service) {
        this.service = service;
    }

    // Publico: con token recomienda segun el usuario; sin token, los mas populares
    @GetMapping("/recomendaciones")
    public List<ProductoDto> recomendaciones(@AuthenticationPrincipal Jwt jwt) {
        return service.recomendarPara(usuarioDesde(jwt));
    }

    // Mismo criterio que ms-productos y ms-carrito para identificar al usuario
    private static String usuarioDesde(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        for (String claim : new String[]{"preferred_username", "upn", "unique_name", "email"}) {
            String valor = jwt.getClaimAsString(claim);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }
}