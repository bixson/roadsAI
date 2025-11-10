package dk.ek.roadsai.controller;


import dk.ek.roadsai.service.RouteService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * hardcoded rvk-isf route endpoint
 */
@RestController
@RequestMapping("/api/route")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping(value = "/rvk-isf", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> rvkIsf() {
        return routeService.getGeoJson();
    }
}
