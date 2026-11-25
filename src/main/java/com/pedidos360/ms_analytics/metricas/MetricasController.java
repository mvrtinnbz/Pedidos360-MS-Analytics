package com.pedidos360.ms_analytics.metricas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class MetricasController {

    private final MetricasService service;

    public MetricasController(MetricasService service) {
        this.service = service;
    }

    @GetMapping("/metricas")
    public MetricasDto metricas() {
        return service.snapshot();
    }
}