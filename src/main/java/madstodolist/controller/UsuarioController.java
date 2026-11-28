package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoAdministradorException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.controller.exception.UsuarioNotFoundException;
import madstodolist.dto.PasswordData;
import madstodolist.dto.PerfilData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import madstodolist.service.UsuarioServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

@Controller
public class UsuarioController {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    // Comprueba que el usuario logeado es el administrador y lo devuelve.
    // Si no hay usuario logeado o no es administrador se devuelve un 401,
    // en el segundo caso con un mensaje de falta de permisos
    private UsuarioData comprobarAdministrador() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null)
            throw new UsuarioNoLogeadoException();

        UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
        if (usuario == null || !usuario.isAdmin())
            throw new UsuarioNoAdministradorException();
        return usuario;
    }

    // Comprueba que hay un usuario logeado y lo devuelve.
    // Si no hay usuario logeado se devuelve un 401
    private UsuarioData comprobarUsuarioLogeado() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null)
            throw new UsuarioNoLogeadoException();

        UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
        if (usuario == null)
            throw new UsuarioNoLogeadoException();
        return usuario;
    }

    @GetMapping("/cuenta")
    public String cuenta(Model model) {

        // Cualquier usuario logeado puede ver los datos de su propia cuenta
        UsuarioData usuario = comprobarUsuarioLogeado();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        return "cuenta";
    }

    @GetMapping("/cuenta/editar")
    public String formEditarPerfil(@ModelAttribute PerfilData perfilData, Model model) {

        // Cada usuario edita su propio perfil
        UsuarioData usuario = comprobarUsuarioLogeado();

        perfilData.setEmail(usuario.getEmail());
        perfilData.setNombre(usuario.getNombre());
        perfilData.setFechaNacimiento(usuario.getFechaNacimiento());

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        return "formEditarPerfil";
    }

    @PostMapping("/cuenta/editar")
    public String grabaPerfilModificado(@Valid PerfilData perfilData, BindingResult result,
                                        Model model, RedirectAttributes flash) {

        UsuarioData usuario = comprobarUsuarioLogeado();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);

        if (result.hasErrors())
            return "formEditarPerfil";

        UsuarioData usuarioModificado = new UsuarioData();
        usuarioModificado.setEmail(perfilData.getEmail());
        usuarioModificado.setNombre(perfilData.getNombre());
        usuarioModificado.setFechaNacimiento(perfilData.getFechaNacimiento());

        try {
            usuarioService.modificaUsuario(usuario.getId(), usuarioModificado);
        } catch (UsuarioServiceException e) {
            // Si los datos no son válidos, volvemos al formulario mostrando el error
            model.addAttribute("error", e.getMessage());
            return "formEditarPerfil";
        }

        flash.addFlashAttribute("mensaje", "Perfil modificado correctamente");
        return "redirect:/cuenta";
    }

    @GetMapping("/cuenta/contrasena")
    public String formCambiarPassword(@ModelAttribute PasswordData passwordData, Model model) {

        // Cada usuario cambia su propia contraseña.
        // Los campos del formulario nunca vienen rellenos
        UsuarioData usuario = comprobarUsuarioLogeado();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        return "formCambiarPassword";
    }

    @GetMapping("/registrados")
    public String listadoUsuarios(Model model) {

        // Solo el administrador puede acceder al listado
        UsuarioData usuario = comprobarAdministrador();
        List<UsuarioData> usuarios = usuarioService.findAllUsuarios();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("usuarios", usuarios);
        return "listaUsuarios";
    }

    @GetMapping("/registrados/{id}")
    public String descripcionUsuario(@PathVariable(value="id") Long idUsuario, Model model) {

        // Solo el administrador puede acceder a la descripción
        UsuarioData usuario = comprobarAdministrador();

        UsuarioData usuarioRegistrado = usuarioService.findById(idUsuario);
        if (usuarioRegistrado == null)
            throw new UsuarioNotFoundException();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("usuarioRegistrado", usuarioRegistrado);
        return "descripcionUsuario";
    }

    @PostMapping("/registrados/{id}/bloquear")
    public String bloquearUsuario(@PathVariable(value="id") Long idUsuario, RedirectAttributes flash) {
        return cambiarBloqueo(idUsuario, true, flash);
    }

    @PostMapping("/registrados/{id}/habilitar")
    public String habilitarUsuario(@PathVariable(value="id") Long idUsuario, RedirectAttributes flash) {
        return cambiarBloqueo(idUsuario, false, flash);
    }

    private String cambiarBloqueo(Long idUsuario, boolean bloqueado, RedirectAttributes flash) {

        // Solo el administrador puede bloquear o habilitar usuarios
        comprobarAdministrador();

        UsuarioData usuarioRegistrado = usuarioService.findById(idUsuario);
        if (usuarioRegistrado == null)
            throw new UsuarioNotFoundException();

        if (usuarioRegistrado.isAdmin()) {
            flash.addFlashAttribute("error", "No se puede bloquear al administrador");
            return "redirect:/registrados";
        }

        usuarioService.setBloqueoUsuario(idUsuario, bloqueado);
        flash.addFlashAttribute("mensaje", "Usuario " + usuarioRegistrado.getEmail() +
                (bloqueado ? " bloqueado" : " habilitado") + " correctamente");
        return "redirect:/registrados";
    }
}
