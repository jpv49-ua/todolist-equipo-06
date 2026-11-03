package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.UsuarioNoAdministradorException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.controller.exception.UsuarioNotFoundException;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping("/cuenta")
    public String cuenta(Model model) {

        // Cualquier usuario logeado puede ver los datos de su propia cuenta
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null)
            throw new UsuarioNoLogeadoException();

        UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
        if (usuario == null)
            throw new UsuarioNoLogeadoException();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        return "cuenta";
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
