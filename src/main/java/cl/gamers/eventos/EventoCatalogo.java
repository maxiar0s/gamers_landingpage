package cl.gamers.eventos;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EventoCatalogo {
    private static final String SELECT_EVENTO = "SELECT slug, nombre, categoria, juego, pais, lugar, fecha, experiencia, resumen, descripcion FROM eventos";
    private static final RowMapper<Evento> EVENTO_MAPPER = (rs, rowNum) -> new Evento(
            rs.getString("slug"), rs.getString("nombre"), rs.getString("categoria"), rs.getString("juego"),
            rs.getString("pais"), rs.getString("lugar"), rs.getString("fecha"), rs.getString("experiencia"),
            rs.getString("resumen"), rs.getString("descripcion"));

    private final JdbcTemplate jdbc;

    public EventoCatalogo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Evento> buscar(String q, String categoria, String juego, String pais, String experiencia) {
        List<String> conditions = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        addContainsFilter(conditions, values, "CONCAT(nombre, ' ', resumen)", q);
        addContainsFilter(conditions, values, "categoria", categoria);
        addContainsFilter(conditions, values, "juego", juego);
        addContainsFilter(conditions, values, "pais", pais);
        addContainsFilter(conditions, values, "experiencia", experiencia);

        String sql = SELECT_EVENTO + (conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions))
                + " ORDER BY id";
        return jdbc.query(sql, EVENTO_MAPPER, values.toArray());
    }

    private void addContainsFilter(List<String> conditions, List<Object> values, String column, String filter) {
        if (filter != null && !filter.isBlank()) {
            conditions.add("LOWER(" + column + ") LIKE ?");
            values.add("%" + filter.trim().toLowerCase(java.util.Locale.ROOT) + "%");
        }
    }

    public List<Evento> recientes() {
        return jdbc.query(SELECT_EVENTO + " ORDER BY id DESC LIMIT 2", EVENTO_MAPPER);
    }

    /** Selección editorial fija; no representa métricas de visitas o popularidad real. */
    public List<Evento> popularesMuestra() {
        return jdbc.query(SELECT_EVENTO + " WHERE slug IN (?, ?) ORDER BY CASE slug WHEN ? THEN 0 WHEN ? THEN 1 END",
                EVENTO_MAPPER, "pixel-santiago", "arena-cosplay", "pixel-santiago", "arena-cosplay");
    }

    public Optional<Evento> porSlug(String slug) {
        return jdbc.query(SELECT_EVENTO + " WHERE slug = ?", EVENTO_MAPPER, slug).stream().findFirst();
    }
}
