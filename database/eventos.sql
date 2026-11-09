-- MySQL 8.0+ | Datos de demostración para la agenda gamer.
CREATE DATABASE IF NOT EXISTS comunidad_gamer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE comunidad_gamer;

CREATE TABLE IF NOT EXISTS eventos (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    nombre VARCHAR(160) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    juego VARCHAR(100) NOT NULL,
    pais VARCHAR(80) NOT NULL,
    lugar VARCHAR(180) NOT NULL,
    fecha VARCHAR(80) NOT NULL,
    experiencia VARCHAR(50) NOT NULL,
    resumen VARCHAR(300) NOT NULL,
    descripcion TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO eventos (slug,nombre,categoria,juego,pais,lugar,fecha,experiencia,resumen,descripcion) VALUES
('pixel-santiago','Festival Pixel Santiago','Festival','Videojuegos','Chile','Centro Cultural GAM','15 de noviembre','Familiar','Encuentro de videojuegos independientes, arcades y comunidad local.','Una jornada para descubrir creadores independientes, jugar clásicos arcade y compartir con comunidades de todo Chile.'),
('arena-cosplay','Arena Cosplay y eSports','Competencia','Valorant','Chile','Movistar Arena, Santiago','22 de noviembre','Avanzada','Finales de eSports y pasarela de cosplay en una misma arena.','Vive las finales regionales, conoce a cosplayers invitados y participa en actividades para la comunidad.'),
('retro-valpo','Retro Gaming Valparaiso','Comunidad','Retro','Chile','Parque Cultural de Valparaiso','29 de noviembre','Principiante','Consolas clásicas, torneos amistosos e historia gamer.','Una experiencia para jugar en familia, aprender sobre la historia de los videojuegos y participar en torneos amistosos.'),
('pixel-mendoza','Conecta Gamer Mendoza','Festival','League of Legends','Argentina','Espacio Cultural Julio Le Parc','6 de diciembre','Intermedia','Torneos, desarrolladores y cultura digital en Mendoza.','Jornada con torneos comunitarios, charlas de desarrollo y encuentros entre jugadores de la región.')
ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), categoria=VALUES(categoria), juego=VALUES(juego), pais=VALUES(pais), lugar=VALUES(lugar), fecha=VALUES(fecha), experiencia=VALUES(experiencia), resumen=VALUES(resumen), descripcion=VALUES(descripcion);
