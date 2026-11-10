package cl.gamers.eventos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EventoWebTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate http;
    @LocalServerPort int port;

    @DynamicPropertySource
    static void productionSessionCookieConfig(DynamicPropertyRegistry registry) throws IOException {
        Properties production = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            production.load(input);
        }
        registry.add("server.servlet.session.cookie.same-site",
                () -> production.getProperty("server.servlet.session.cookie.same-site"));
    }

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

    @Test void publicResponsesSetContentSecurityPolicy() throws Exception {
        mvc.perform(get("/eventos")).andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }

    @Test void productionSessionCookieUsesLaxSameSiteAndAuthenticatedSessionWorks() {
        String baseUrl = "http://localhost:" + port;
        var loginPage = http.getForEntity(baseUrl + "/login", String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, loginPage.getStatusCode().value());
        String initialCookie = loginPage.getHeaders().getFirst("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(initialCookie, loginPage.getHeaders().toString());
        org.junit.jupiter.api.Assertions.assertTrue(initialCookie.startsWith("JSESSIONID="), initialCookie);
        org.junit.jupiter.api.Assertions.assertTrue(initialCookie.contains("SameSite=Lax"), initialCookie);

        java.util.regex.Matcher csrf = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(loginPage.getBody());
        org.junit.jupiter.api.Assertions.assertTrue(csrf.find(), loginPage.getBody());
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add(org.springframework.http.HttpHeaders.COOKIE, initialCookie.split(";", 2)[0]);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);
        org.springframework.util.MultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
        form.add("username", "player");
        form.add("password", "pixel123");
        form.add("_csrf", csrf.group(1));
        var login = http.postForEntity(baseUrl + "/login",
                new org.springframework.http.HttpEntity<>(form, headers), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(302, login.getStatusCode().value());
        String authenticatedCookie = login.getHeaders().getFirst("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(authenticatedCookie, login.getHeaders().toString());
        org.junit.jupiter.api.Assertions.assertTrue(authenticatedCookie.startsWith("JSESSIONID="), authenticatedCookie);
        org.junit.jupiter.api.Assertions.assertTrue(authenticatedCookie.contains("SameSite=Lax"), authenticatedCookie);
        org.springframework.http.HttpHeaders authenticatedHeaders = new org.springframework.http.HttpHeaders();
        authenticatedHeaders.add(org.springframework.http.HttpHeaders.COOKIE, authenticatedCookie.split(";", 2)[0]);
        var details = http.exchange(baseUrl + "/eventos/pixel-santiago", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(authenticatedHeaders), String.class);
        org.junit.jupiter.api.Assertions.assertEquals(200, details.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertTrue(details.getBody().contains("Centro Cultural GAM"));
    }

    @Test void searchInputIsEscapedWhenRenderedInHtmlAttribute() throws Exception {
        String payload = "\" autofocus onfocus=\"alert(1)";
        String html = mvc.perform(get("/eventos").param("q", payload)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("value=\"&quot; autofocus onfocus=&quot;alert(1)\""), html);
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("value=\"\" autofocus"), html);
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
