package dk.ek.roadsai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving frontend views.
 * Routes V2 requests to the correct static files.
 */
@Controller
public class ViewsController {

    @GetMapping("/v2")
    public String v2Redirect() {
        return "redirect:/v2/";
    }

    @GetMapping("/v2/")
    public String v2() {
        return "forward:/v2/index.html";
    }
}
