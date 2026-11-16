--
-- Migración del esquema de la versión 1.2.0 a la 1.3.0
-- Añade el comentario (opcional, uno por tarea) a la tabla de tareas
--

ALTER TABLE public.tareas ADD COLUMN comentario character varying(255);
-- Tareas de equipo: columna equipo_id y su clave ajena a equipos
ALTER TABLE public.tareas ADD COLUMN equipo_id bigint;

ALTER TABLE public.tareas
    ADD CONSTRAINT fk_tareas_equipo FOREIGN KEY (equipo_id) REFERENCES public.equipos(id);
