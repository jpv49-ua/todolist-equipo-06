--
-- Migración del esquema de la versión 1.2.0 a la 1.3.0
--
-- Cambios:
--   1. Comentario (opcional, uno por tarea) en la tabla de tareas
--   2. Equipo (opcional) al que pertenece una tarea
--   3. El responsable de una tarea pasa a ser opcional
--

-- 1. Comentario de la tarea
ALTER TABLE public.tareas ADD COLUMN comentario character varying(255);

-- 2. Tareas de equipo: columna equipo_id y su clave ajena a equipos
ALTER TABLE public.tareas ADD COLUMN equipo_id bigint;

ALTER TABLE public.tareas
    ADD CONSTRAINT fk_tareas_equipo FOREIGN KEY (equipo_id) REFERENCES public.equipos(id);

-- 3. El responsable de una tarea de equipo es opcional
ALTER TABLE public.tareas ALTER COLUMN usuario_id DROP NOT NULL;