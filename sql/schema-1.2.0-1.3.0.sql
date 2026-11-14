--
-- Migración del esquema de la versión 1.2.0 a la 1.3.0
-- Añade el comentario (opcional, uno por tarea) a la tabla de tareas
--

ALTER TABLE public.tareas ADD COLUMN comentario character varying(255);
