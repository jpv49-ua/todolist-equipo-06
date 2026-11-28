package madstodolist.service;

import madstodolist.dto.UsuarioData;
import madstodolist.model.Usuario;
import madstodolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UsuarioService {

    public enum LoginStatus {LOGIN_OK, USER_NOT_FOUND, ERROR_PASSWORD, USER_BLOCKED}

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public LoginStatus login(String eMail, String password) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(eMail);
        if (!usuario.isPresent()) {
            return LoginStatus.USER_NOT_FOUND;
        } else if (!usuario.get().getPassword().equals(password)) {
            return LoginStatus.ERROR_PASSWORD;
        } else if (usuario.get().isBloqueado()) {
            return LoginStatus.USER_BLOCKED;
        } else {
            return LoginStatus.LOGIN_OK;
        }
    }

    // Se añade un usuario en la aplicación.
    // El email y password del usuario deben ser distinto de null
    // El email no debe estar registrado en la base de datos
    // Solo puede existir un usuario administrador
    @Transactional
    public UsuarioData registrar(UsuarioData usuario) {
        Optional<Usuario> usuarioBD = usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioBD.isPresent())
            throw new UsuarioServiceException("El usuario " + usuario.getEmail() + " ya está registrado");
        else if (usuario.getEmail() == null)
            throw new UsuarioServiceException("El usuario no tiene email");
        else if (usuario.getPassword() == null)
            throw new UsuarioServiceException("El usuario no tiene password");
        else if (usuario.isAdmin() && existeAdministrador())
            throw new UsuarioServiceException("Ya existe un usuario administrador");
        else {
            Usuario usuarioNuevo = modelMapper.map(usuario, Usuario.class);
            usuarioNuevo = usuarioRepository.save(usuarioNuevo);
            return modelMapper.map(usuarioNuevo, UsuarioData.class);
        }
    }

    @Transactional(readOnly = true)
    public UsuarioData findByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) return null;
        else {
            return modelMapper.map(usuario, UsuarioData.class);
        }
    }

    @Transactional(readOnly = true)
    public UsuarioData findById(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) return null;
        else {
            return modelMapper.map(usuario, UsuarioData.class);
        }
    }

    // Devuelve todos los usuarios registrados ordenados por id
    @Transactional(readOnly = true)
    public List<UsuarioData> findAllUsuarios() {
        return StreamSupport.stream(usuarioRepository.findAll().spliterator(), false)
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .collect(Collectors.toList());
    }

    // Indica si ya hay un usuario administrador registrado
    @Transactional(readOnly = true)
    public boolean existeAdministrador() {
        return usuarioRepository.existsByAdminTrue();
    }

    // Bloquea o habilita el acceso de un usuario.
    // El usuario debe existir y no se puede bloquear al administrador
    @Transactional
    public UsuarioData setBloqueoUsuario(Long usuarioId, boolean bloqueado) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null)
            throw new UsuarioServiceException("No existe usuario con id " + usuarioId);
        if (usuario.isAdmin() && bloqueado)
            throw new UsuarioServiceException("No se puede bloquear al administrador");
        usuario.setBloqueado(bloqueado);
        usuario = usuarioRepository.save(usuario);
        return modelMapper.map(usuario, UsuarioData.class);
    }

    // Modifica los datos del perfil de un usuario: email, nombre y fecha de nacimiento.
    // El usuario debe existir, el email no puede ser vacío
    // y no puede pertenecer ya a otro usuario.
    // No se modifican la contraseña ni los atributos admin y bloqueado.
    @Transactional
    public UsuarioData modificaUsuario(Long usuarioId, UsuarioData usuarioModificado) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null)
            throw new UsuarioServiceException("No existe usuario con id " + usuarioId);

        String email = usuarioModificado.getEmail();
        if (email == null || email.trim().isEmpty())
            throw new UsuarioServiceException("El email no puede estar vacío");

        email = email.trim();
        Optional<Usuario> usuarioConEseEmail = usuarioRepository.findByEmail(email);
        if (usuarioConEseEmail.isPresent() && !usuarioConEseEmail.get().getId().equals(usuarioId))
            throw new UsuarioServiceException("El email " + email + " ya está registrado");

        usuario.setEmail(email);
        usuario.setNombre(usuarioModificado.getNombre());
        usuario.setFechaNacimiento(usuarioModificado.getFechaNacimiento());
        usuario = usuarioRepository.save(usuario);
        return modelMapper.map(usuario, UsuarioData.class);
    }

    // Cambia la contraseña de un usuario.
    // La contraseña actual indicada debe ser la correcta
    // y la nueva no puede estar vacía (se guarda tal cual, sin recortar).
    // No se modifica ningún otro dato del usuario.
    @Transactional
    public UsuarioData cambiaPassword(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (!usuario.getPassword().equals(passwordActual))
            throw new UsuarioServiceException("La contraseña actual no es correcta");
        if (passwordNueva == null || passwordNueva.trim().isEmpty())
            throw new UsuarioServiceException("La contraseña nueva no puede estar vacía");
        usuario.setPassword(passwordNueva);
        usuario = usuarioRepository.save(usuario);
        return modelMapper.map(usuario, UsuarioData.class);
    }
}
