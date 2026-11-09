package cl.gamers.eventos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class EventoWebTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void homeIsPublicAndShowsSearchableStaticEventSummaries() throws Exception {
        mvc.perform(get("/eventos")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Festival Pixel Santiago")))
                .andExpect(content().string(containsString("Buscar eventos")));
        String pixelResults = resultsFor(get("/eventos").param("q", "Pixel"));
        org.junit.jupiter.api.Assertions.assertTrue(pixelResults.contains("Festival Pixel Santiago"));
        org.junit.jupiter.api.Assertions.assertFalse(pixelResults.contains("Arena Cosplay"));
    }

    @Test @Transactional void emptyDatabaseRendersAnEmptyAgendaWithoutCrashing() throws Exception {
        jdbc.update("DELETE FROM eventos");
        mvc.perform(get("/eventos")).andExpect(status().isOk())
                .andExpect(content().string(containsString("0 eventos encontrados")))
                .andExpect(content().string(containsString("No encontramos eventos")));
    }

    @Test void databaseEventsAreVisibleThroughPublicSearchAndProtectedDetails() throws Exception {
        jdbc.update("INSERT INTO eventos (slug,nombre,categoria,juego,pais,lugar,fecha,experiencia,resumen,descripcion) VALUES (?,?,?,?,?,?,?,?,?,?)",
                "database-proof", "Evento Solo en BD", "Torneo", "Minecraft", "Chile", "Centro Gamer", "10 de enero", "Principiante", "Fila leída desde MySQL-compatible DB.", "Detalle cargado desde la base de datos.");
        mvc.perform(get("/eventos").param("q", "Solo en BD")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Evento Solo en BD")));
        mvc.perform(get("/eventos/database-proof")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
        mvc.perform(get("/eventos/database-proof").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("player")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Detalle cargado desde la base de datos.")));
    }

    @Test void detailsRequireAuthenticationAndAuthenticatedUserCanSeeDetails() throws Exception {
        mvc.perform(get("/eventos/pixel-santiago")).andExpect(status().is3xxRedirection()).andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
        mvc.perform(get("/eventos/pixel-santiago").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("player")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Centro Cultural GAM")));
    }

    @Test void loginPageAndDemoAccountAuthenticate() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(containsString("name=\"username\"")));
        mvc.perform(post("/login").with(csrf()).param("username", "player").param("password", "pixel123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }

    @Test void unspecifiedRoutesRequireAuthenticationByDefault() throws Exception {
        mvc.perform(get("/ruta-no-publicada")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
    }

    @Test void allDemoAccountsAuthenticateAndIncorrectPasswordIsRejected() throws Exception {
        String[][] accounts = {{"player", "pixel123"}, {"organizador", "arena123"}, {"invitado", "retro123"}};
        for (String[] account : accounts) {
            mvc.perform(post("/login").with(csrf()).param("username", account[0]).param("password", account[1]))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        }
        mvc.perform(post("/login").with(csrf()).param("username", "player").param("password", "incorrecta"))
                .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login?error")));
    }

    @Test void everyNamedSearchFilterWorksAndCombinedFiltersPreserveMatching() throws Exception {
        assertSearch("categoria", "Competencia", "Arena Cosplay y eSports", "Festival Pixel Santiago");
        assertSearch("juego", "Valorant", "Arena Cosplay y eSports", "Festival Pixel Santiago");
        assertSearch("pais", "Argentina", "Conecta Gamer Mendoza", "Festival Pixel Santiago");
        assertSearch("experiencia", "Principiante", "Retro Gaming Valparaiso", "Festival Pixel Santiago");
        String combinedResults = resultsFor(get("/eventos").param("categoria", "Festival").param("pais", "Chile"));
        org.junit.jupiter.api.Assertions.assertTrue(combinedResults.contains("Festival Pixel Santiago"));
        org.junit.jupiter.api.Assertions.assertFalse(combinedResults.contains("Conecta Gamer Mendoza"));
        org.junit.jupiter.api.Assertions.assertFalse(combinedResults.contains("Arena Cosplay y eSports"));
    }

    @Test void homeLabelsSampleRecentPopularAndPromotionalContentHonestly() throws Exception {
        mvc.perform(get("/eventos")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Eventos recientes")))
                .andExpect(content().string(containsString("Populares · selección editorial de muestra")))
                .andExpect(content().string(containsString("Promoción de muestra")))
                .andExpect(content().string(containsString("sin patrocinio real")));
    }

    private String resultsFor(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        String html = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int start = html.indexOf("<section class=\"results\"");
        int end = html.indexOf("</section>", start);
        org.junit.jupiter.api.Assertions.assertTrue(start >= 0 && end > start, "Results section must be rendered");
        return html.substring(start, end);
    }

    private void assertSearch(String filter, String value, String included, String excluded) throws Exception {
        String results = resultsFor(get("/eventos").param(filter, value));
        org.junit.jupiter.api.Assertions.assertTrue(results.contains(included), results);
        org.junit.jupiter.api.Assertions.assertFalse(results.contains(excluded), results);
    }

    @Test void stateChangingRequestsRequireCsrfToken() throws Exception {
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("player")).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    }
}
