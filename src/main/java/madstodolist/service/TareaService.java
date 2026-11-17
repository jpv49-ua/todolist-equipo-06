package madstodolist.service;

import madstodolist.model.EstadoTareaEnum;
import madstodolist.model.Tarea;
import madstodolist.repository.TareaRepository;
import madstodolist.model.Usuario;
import madstodolist.repository.UsuarioRepository;
import madstodolist.dto.TareaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.modelmapper.ModelMapper;
import madstodolist.model.Equipo;
import madstodolist.repository.EquipoRepository;

import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;


@Service
public class TareaService {

    Logger logger = LoggerFactory.getLogger(TareaService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TareaRepository tareaRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private EquipoRepository equipoRepository;

    @Transactional
    public TareaData nuevaTareaUsuario(Long idUsuario, String tituloTarea) {
        logger.debug("Añadiendo tarea " + tituloTarea + " al usuario " + idUsuario);
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) {
            throw new TareaServiceException("Usuario " + idUsuario + " no existe al crear tarea " + tituloTarea);
        }
        Tarea tarea = new Tarea(usuario, tituloTarea);
        tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }

    // Crea una tarea asociada a un equipo. El responsable es opcional:
    // si es null, la tarea queda sin asignar.
    @Transactional
    public TareaData nuevaTareaEquipo(Long idEquipo, Long idResponsable, String tituloTarea) {
        logger.debug("Añadiendo tarea " + tituloTarea + " al equipo " + idEquipo);
        if (tituloTarea == null || tituloTarea.trim().isEmpty()) {
            throw new TareaServiceException("El título de la tarea no puede estar vacío");
        }
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null) {
            throw new TareaServiceException("Equipo " + idEquipo + " no existe al crear tarea " + tituloTarea);
        }
        Tarea tarea = new Tarea(equipo, tituloTarea.trim());
        if (idResponsable != null) {
            Usuario responsable = usuarioRepository.findById(idResponsable).orElse(null);
            if (responsable == null) {
                throw new TareaServiceException("Usuario " + idResponsable + " no existe al crear tarea " + tituloTarea);
            }
            if (!equipo.getUsuarios().contains(responsable)) {
                throw new TareaServiceException("Usuario " + idResponsable + " no pertenece al equipo " + idEquipo);
            }
            tarea.setUsuario(responsable);
        }
        tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }

    @Transactional(readOnly = true)
    public List<TareaData> allTareasEquipo(Long idEquipo) {

        //logger.debug("Devolviendo todas las tareas del equipo " + idEquipo);

        if(!equipoRepository.findById(idEquipo).isPresent()) {

            throw new TareaServiceException("Equipo " + idEquipo + " no existe al listar tareas");
        }

        return tareaRepository.findByEquipoIdOrderByIdAsc(idEquipo).stream()
                .map(tarea -> modelMapper.map(tarea, TareaData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TareaData> allTareasUsuario(Long idUsuario) {
        //logger.debug("Devolviendo las tareas personales del usuario " + idUsuario);

        if(!usuarioRepository.findById(idUsuario).isPresent()) {

            throw new TareaServiceException("Usuario " + idUsuario + " no existe al listar tareas ");
        }

        return tareaRepository.findByUsuarioIdAndEquipoIsNullOrderByIdAsc(idUsuario).stream()
                .map(tarea -> modelMapper.map(tarea, TareaData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TareaData findById(Long tareaId) {
        logger.debug("Buscando tarea " + tareaId);
        Tarea tarea = tareaRepository.findById(tareaId).orElse(null);
        if (tarea == null) return null;
        else return modelMapper.map(tarea, TareaData.class);
    }

    @Transactional
    public TareaData modificaTarea(Long idTarea, String nuevoTitulo) {
        logger.debug("Modificando tarea " + idTarea + " - " + nuevoTitulo);
        Tarea tarea = tareaRepository.findById(idTarea).orElse(null);
        if (tarea == null) {
            throw new TareaServiceException("No existe tarea con id " + idTarea);
        }
        tarea.setTitulo(nuevoTitulo);
        tarea = tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }

    // Cada tarea tiene como máximo un comentario: si ya tenía uno, se sustituye
    @Transactional
    public TareaData guardaComentario(Long idTarea, String comentario) {
        logger.debug("Guardando comentario de la tarea " + idTarea);
        if (comentario == null || comentario.trim().isEmpty()) {
            throw new TareaServiceException("El comentario no puede estar vacío");
        }
        Tarea tarea = tareaRepository.findById(idTarea).orElse(null);
        if (tarea == null) {
            throw new TareaServiceException("No existe tarea con id " + idTarea);
        }
        tarea.setComentario(comentario.trim());
        tarea = tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }

    @Transactional
    public TareaData borraComentario(Long idTarea) {
        logger.debug("Borrando comentario de la tarea " + idTarea);
        Tarea tarea = tareaRepository.findById(idTarea).orElse(null);
        if (tarea == null) {
            throw new TareaServiceException("No existe tarea con id " + idTarea);
        }
        tarea.setComentario(null);
        tarea = tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }

    @Transactional
    public void borraTarea(Long idTarea) {
        logger.debug("Borrando tarea " + idTarea);
        Tarea tarea = tareaRepository.findById(idTarea).orElse(null);
        if (tarea == null) {
            throw new TareaServiceException("No existe tarea con id " + idTarea);
        }
        tareaRepository.delete(tarea);
    }

    @Transactional
    public boolean usuarioContieneTarea(Long usuarioId, Long tareaId) {
        Tarea tarea = tareaRepository.findById(tareaId).orElse(null);
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (tarea == null || usuario == null) {
            throw new TareaServiceException("No existe tarea o usuario id");
        }
        return usuario.getTareas().contains(tarea);
    }

    // Modifica el título y el responsable de una tarea de equipo.
    // El nuevo responsable debe pertenecer al equipo de la tarea.
    @Transactional
    public TareaData modificaTareaEquipo(Long idTarea, String nuevoTitulo, Long idResponsable) {

        //logger.debug("Modificando la tarea de equipo " + idTarea);

        if(nuevoTitulo == null || nuevoTitulo.trim().isEmpty()) {

            throw new TareaServiceException("El título de la tarea no puede estar vacío");
        }

        Tarea tarea = tareaRepository.findById(idTarea).orElse(null);
        if(tarea == null) {

            throw new TareaServiceException("No existe tarea con id " + idTarea);
        }

        Equipo equipo = tarea.getEquipo();
        if(equipo == null) {

            throw new TareaServiceException("La tarea " + idTarea + " no pertenece a ningún equipo");
        }

        tarea.setTitulo(nuevoTitulo.trim());

        if (idResponsable == null) {

            // La tarea se queda sin asignar
            tarea.setUsuario(null);
        } else {

            Usuario responsable = usuarioRepository.findById(idResponsable).orElse(null);
            if(responsable == null) {

                throw new TareaServiceException("Usuario " + idResponsable + " no existe");
            }
            if(!equipo.getUsuarios().contains(responsable)) {

                throw new TareaServiceException("Usuario " + idResponsable + " no pertenece al equipo " + equipo.getId());
            }

            tarea.setUsuario(responsable);
        }

        tarea = tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }

    @Transactional
    public TareaData cambiaEstadoTarea(Long idTarea, EstadoTareaEnum nuevoEstado) {
        logger.debug("Cambiando el estado de la tarea " + idTarea + " a " + nuevoEstado);
        Tarea tarea = tareaRepository.findById(idTarea).get();
        tarea.setEstado(nuevoEstado);
        tarea = tareaRepository.save(tarea);
        return modelMapper.map(tarea, TareaData.class);
    }
}
