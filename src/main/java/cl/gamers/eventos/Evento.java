package cl.gamers.eventos;

public record Evento(String slug, String nombre, String categoria, String juego, String pais,
                     String lugar, String fecha, String experiencia, String resumen, String descripcion) { }
