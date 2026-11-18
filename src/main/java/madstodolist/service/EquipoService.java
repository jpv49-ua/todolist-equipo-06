package madstodolist.service;

import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.model.Equipo;
import madstodolist.model.Usuario;
import madstodolist.repository.EquipoRepository;
import madstodolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    @Autowired
    EquipoRepository equipoRepository;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    private ModelMapper modelMapper;

    private void comprobarNombreValido(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new EquipoServiceException("El nombre del equipo no puede estar vacío");
        }
    }

    @Transactional
    public EquipoData crearEquipo(String nombre) {
        comprobarNombreValido(nombre);
        Equipo equipo = new Equipo(nombre);
        equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public EquipoData modificarNombreEquipo(Long id, String nombre) {
        comprobarNombreValido(nombre);
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("Equipo " + id + " no existe");
        }
        equipo.setNombre(nombre);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public void eliminarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("Equipo " + id + " no existe");
        }
        // Hay que actualizar ambas colecciones antes de borrar, porque
        // JPA/Hibernate no lo hace automáticamente
        new ArrayList<>(equipo.getUsuarios()).forEach(equipo::removeUsuario);
        equipoRepository.delete(equipo);
    }

    @Transactional(readOnly = true)
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("Equipo " + id + " no existe");
        }
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional(readOnly = true)
    public List<EquipoData> findAllOrdenadoPorNombre() {
        return equipoRepository.findAll().stream()
                .sorted(Comparator.comparing(Equipo::getNombre))
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public void añadirUsuarioAEquipo(Long id, Long id1) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("Equipo " + id + " no existe");
        }
        Usuario usuario = usuarioRepository.findById(id1).orElse(null);
        if (usuario == null) {
            throw new EquipoServiceException("Usuario " + id1 + " no existe");
        }
        if (equipo.getUsuarios().contains(usuario)) {
            throw new EquipoServiceException("Usuario " + id1 + " ya pertenece al equipo " + id);
        }
        equipo.addUsuario(usuario);
    }

    @Transactional
    public void eliminarUsuarioDeEquipo(Long equipoId, Long usuarioId) {
        Equipo equipo = equipoRepository.findById(equipoId).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("Equipo " + equipoId + " no existe");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            throw new EquipoServiceException("Usuario " + usuarioId + " no existe");
        }
        if (!equipo.getUsuarios().contains(usuario)) {
            throw new EquipoServiceException("Usuario " + usuarioId + " no pertenece al equipo " + equipoId);
        }
        equipo.removeUsuario(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioData> usuariosEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null) {
            throw new EquipoServiceException("Equipo " + id + " no existe");
        }
        return equipo.getUsuarios().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean esMiembroDeEquipo(Long equipoId, Long usuarioId) {

        Equipo equipo = equipoRepository.findById(equipoId).orElse(null);
        if(equipo == null) {

            throw new EquipoServiceException("Equipo " + equipoId + " no existe");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if(usuario == null) {

            throw new EquipoServiceException("Usuario " + usuarioId + " no existe");
        }

        return equipo.getUsuarios().contains(usuario);
    }

    @Transactional(readOnly = true)
    public List<EquipoData> equiposUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            throw new EquipoServiceException("Usuario " + id + " no existe");
        }
        return usuario.getEquipos().stream()
                .sorted(Comparator.comparing(Equipo::getNombre))
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
    }
}
