package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.UsuarioNoAdministradorException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class EquipoController {

    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    ManagerUserSession managerUserSession;

    // Comprueba que hay un usuario logeado y lo devuelve.
    // Si no lo hay se devuelve un 401
    private UsuarioData comprobarUsuarioLogeado() {
        Long idUsuarioLogeado = managerUserSession.usuarioLogeado();
        if (idUsuarioLogeado == null)
            throw new UsuarioNoLogeadoException();

        UsuarioData usuario = usuarioService.findById(idUsuarioLogeado);
        if (usuario == null)
            throw new UsuarioNoLogeadoException();
        return usuario;
    }

    // Comprueba que el usuario logeado es el administrador y lo devuelve.
    // Si no hay usuario logeado o no es administrador se devuelve un 401
    private UsuarioData comprobarAdministrador() {
        UsuarioData usuario = comprobarUsuarioLogeado();
        if (!usuario.isAdmin())
            throw new UsuarioNoAdministradorException();
        return usuario;
    }

    @GetMapping("/equipos")
    public String listadoEquipos(Model model) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipos", equipos);
        model.addAttribute("idsEquiposUsuario", equipoService.equiposUsuario(usuario.getId()).stream()
                .map(EquipoData::getId)
                .collect(Collectors.toSet()));
        return "listaEquipos";
    }

    @GetMapping("/equipos/{id}")
    public String detalleEquipo(@PathVariable(value="id") Long idEquipo, Model model) {
        UsuarioData usuario = comprobarUsuarioLogeado();

        EquipoData equipo;
        List<UsuarioData> miembros;
        try {
            equipo = equipoService.recuperarEquipo(idEquipo);
            miembros = equipoService.usuariosEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipo", equipo);
        model.addAttribute("miembros", miembros);
        model.addAttribute("esMiembro", miembros.stream().anyMatch(m -> m.getId().equals(usuario.getId())));
        return "descripcionEquipo";
    }

    @GetMapping("/equipos/nuevo")
    public String formNuevoEquipo(Model model) {
        UsuarioData usuario = comprobarUsuarioLogeado();

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipoData", new EquipoData());
        return "formNuevoEquipo";
    }

    @PostMapping("/equipos/nuevo")
    public String nuevoEquipo(@ModelAttribute EquipoData equipoData, Model model, RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();

        try {
            EquipoData equipo = equipoService.crearEquipo(equipoData.getNombre());
            equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());
        } catch (EquipoServiceException e) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("logeado", true);
            model.addAttribute("error", e.getMessage());
            return "formNuevoEquipo";
        }
        flash.addFlashAttribute("mensaje", "Equipo creado correctamente");
        return "redirect:/equipos";
    }

    @PostMapping("/equipos/{id}/unirse")
    public String unirseAEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();

        try {
            equipoService.añadirUsuarioAEquipo(idEquipo, usuario.getId());
            flash.addFlashAttribute("mensaje", "Te has unido al equipo correctamente");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos/" + idEquipo;
    }

    @PostMapping("/equipos/{id}/salir")
    public String salirDeEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();

        try {
            equipoService.eliminarUsuarioDeEquipo(idEquipo, usuario.getId());
            flash.addFlashAttribute("mensaje", "Has salido del equipo correctamente");
        } catch (EquipoServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos/" + idEquipo;
    }

    @GetMapping("/equipos/{id}/editar")
    public String formEditarEquipo(@PathVariable(value="id") Long idEquipo, Model model) {
        UsuarioData usuario = comprobarAdministrador();

        EquipoData equipo;
        try {
            equipo = equipoService.recuperarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipoData", equipo);
        return "formEditarEquipo";
    }

    @PostMapping("/equipos/{id}/editar")
    public String editarEquipo(@PathVariable(value="id") Long idEquipo, @ModelAttribute EquipoData equipoData,
                               Model model, RedirectAttributes flash) {
        UsuarioData usuario = comprobarAdministrador();

        try {
            equipoService.recuperarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }

        try {
            equipoService.modificarNombreEquipo(idEquipo, equipoData.getNombre());
        } catch (EquipoServiceException e) {
            equipoData.setId(idEquipo);
            model.addAttribute("usuario", usuario);
            model.addAttribute("logeado", true);
            model.addAttribute("equipoData", equipoData);
            model.addAttribute("error", e.getMessage());
            return "formEditarEquipo";
        }
        flash.addFlashAttribute("mensaje", "Equipo modificado correctamente");
        return "redirect:/equipos";
    }

    @PostMapping("/equipos/{id}/eliminar")
    public String eliminarEquipo(@PathVariable(value="id") Long idEquipo, RedirectAttributes flash) {
        comprobarAdministrador();

        try {
            equipoService.eliminarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
        flash.addFlashAttribute("mensaje", "Equipo eliminado correctamente");
        return "redirect:/equipos";
    }
}
