package cl.gamers.eventos;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class EventoController {
    private final EventoCatalogo catalogo;
    public EventoController(EventoCatalogo catalogo) { this.catalogo = catalogo; }

    @GetMapping({"/", "/eventos"})
    public String eventos(@RequestParam(required = false, defaultValue = "") String q,
                          @RequestParam(required = false, defaultValue = "") String categoria,
                          @RequestParam(required = false, defaultValue = "") String juego,
                          @RequestParam(required = false, defaultValue = "") String pais,
                          @RequestParam(required = false, defaultValue = "") String experiencia, Model model) {
        model.addAttribute("eventos", catalogo.buscar(q, categoria, juego, pais, experiencia));
        model.addAttribute("recientes", catalogo.recientes());
        model.addAttribute("populares", catalogo.popularesMuestra());
        model.addAttribute("q", q); model.addAttribute("categoria", categoria); model.addAttribute("juego", juego);
        model.addAttribute("pais", pais); model.addAttribute("experiencia", experiencia);
        return "eventos";
    }

    @GetMapping("/eventos/{slug}")
    public String detalle(@PathVariable String slug, Model model) {
        model.addAttribute("evento", catalogo.porSlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "detalle";
    }

    @GetMapping("/login") public String login() { return "login"; }
}
