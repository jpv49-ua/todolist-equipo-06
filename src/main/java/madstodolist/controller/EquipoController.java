package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.controller.exception.EquipoNotFoundException;
import madstodolist.controller.exception.TareaNotFoundException;
import madstodolist.controller.exception.UsuarioNoAdministradorException;
import madstodolist.controller.exception.UsuarioNoLogeadoException;
import madstodolist.controller.exception.UsuarioNoMiembroEquipoException;
import madstodolist.dto.EquipoData;
import madstodolist.dto.TareaData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.EquipoServiceException;
import madstodolist.service.TareaService;
import madstodolist.service.TareaServiceException;
import madstodolist.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
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

    @Autowired
    TareaService tareaService;

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

    // Comprueba que el equipo existe (404 si no) y que el usuario logeado
    // es miembro del equipo (401 si no), y devuelve el equipo
    private EquipoData comprobarMiembroDeEquipo(Long idEquipo, UsuarioData usuario) {
        EquipoData equipo;
        try {
            equipo = equipoService.recuperarEquipo(idEquipo);
        } catch (EquipoServiceException e) {
            throw new EquipoNotFoundException();
        }
        if (!equipoService.esMiembroDeEquipo(idEquipo, usuario.getId())) {
            throw new UsuarioNoMiembroEquipoException();
        }
        return equipo;
    }

    // Comprueba que la tarea existe y pertenece al equipo indicado (404 si no)
    private TareaData comprobarTareaDeEquipo(Long idEquipo, Long idTarea) {
        TareaData tarea = tareaService.findById(idTarea);
        if (tarea == null || !idEquipo.equals(tarea.getEquipoId())) {
            throw new TareaNotFoundException();
        }
        return tarea;
    }

    // Devuelve los miembros del equipo ordenados por nombre (o email si no tienen)
    private List<UsuarioData> miembrosOrdenados(Long idEquipo) {
        List<UsuarioData> miembros = equipoService.usuariosEquipo(idEquipo);
        miembros.sort(Comparator.comparing((UsuarioData m) ->
                m.getNombre() != null ? m.getNombre() : m.getEmail(), String.CASE_INSENSITIVE_ORDER));
        return miembros;
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

        boolean esMiembro = miembros.stream().anyMatch(m -> m.getId().equals(usuario.getId()));

        // Ordenamos los miembros: primero el usuario logeado, después por nombre
        miembros.sort(Comparator.comparing((UsuarioData m) -> !m.getId().equals(usuario.getId()))
                .thenComparing(m -> m.getNombre() != null ? m.getNombre() : m.getEmail(),
                        String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipo", equipo);
        model.addAttribute("miembros", miembros);
        model.addAttribute("esMiembro", esMiembro);

        // Las tareas del equipo solo las ven sus miembros
        if (esMiembro) {
            List<TareaData> tareas = tareaService.allTareasEquipo(idEquipo);
            model.addAttribute("tareas", tareas);

            // Agrupadas por responsable, dejando fuera las que no tienen
            model.addAttribute("tareasPorMiembro", tareas.stream()
                    .filter(t -> t.getUsuarioId() != null)
                    .collect(Collectors.groupingBy(TareaData::getUsuarioId)));

            model.addAttribute("tareasSinAsignar", tareas.stream()
                    .filter(t -> t.getUsuarioId() == null)
                    .collect(Collectors.toList()));
        }

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

    @GetMapping("/equipos/{id}/tareas/nueva")
    public String formNuevaTareaEquipo(@PathVariable(value="id") Long idEquipo,
                                       @ModelAttribute TareaData tareaData, Model model) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        EquipoData equipo = comprobarMiembroDeEquipo(idEquipo, usuario);

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipo", equipo);
        model.addAttribute("miembros", miembrosOrdenados(idEquipo));
        return "formNuevaTareaEquipo";
    }

    @PostMapping("/equipos/{id}/tareas/nueva")
    public String nuevaTareaEquipo(@PathVariable(value="id") Long idEquipo,
                                   @ModelAttribute TareaData tareaData, RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        comprobarMiembroDeEquipo(idEquipo, usuario);

        try {
            tareaService.nuevaTareaEquipo(idEquipo, tareaData.getUsuarioId(), tareaData.getTitulo());
        } catch (TareaServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/equipos/" + idEquipo + "/tareas/nueva";
        }
        flash.addFlashAttribute("mensaje", "Tarea añadida al equipo correctamente");
        return "redirect:/equipos/" + idEquipo;
    }

    @GetMapping("/equipos/{id}/tareas/{idTarea}/editar")
    public String formEditarTareaEquipo(@PathVariable(value="id") Long idEquipo,
                                        @PathVariable(value="idTarea") Long idTarea, Model model) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        EquipoData equipo = comprobarMiembroDeEquipo(idEquipo, usuario);
        TareaData tarea = comprobarTareaDeEquipo(idEquipo, idTarea);

        model.addAttribute("usuario", usuario);
        model.addAttribute("logeado", true);
        model.addAttribute("equipo", equipo);
        model.addAttribute("miembros", miembrosOrdenados(idEquipo));
        model.addAttribute("tareaData", tarea);
        return "formEditarTareaEquipo";
    }

    @PostMapping("/equipos/{id}/tareas/{idTarea}/editar")
    public String editarTareaEquipo(@PathVariable(value="id") Long idEquipo,
                                    @PathVariable(value="idTarea") Long idTarea,
                                    @ModelAttribute TareaData tareaData, RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        comprobarMiembroDeEquipo(idEquipo, usuario);
        comprobarTareaDeEquipo(idEquipo, idTarea);

        try {
            tareaService.modificaTareaEquipo(idTarea, tareaData.getTitulo(), tareaData.getUsuarioId());
        } catch (TareaServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/equipos/" + idEquipo + "/tareas/" + idTarea + "/editar";
        }
        flash.addFlashAttribute("mensaje", "Tarea modificada correctamente");
        return "redirect:/equipos/" + idEquipo;
    }

    // Borra una tarea del equipo. Solo puede hacerlo un miembro del equipo.
    @PostMapping("/equipos/{id}/tareas/{idTarea}/borrar")
    public String borrarTareaDeEquipo(@PathVariable(value="id") Long idEquipo,
                                      @PathVariable(value="idTarea") Long idTarea,
                                      RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        comprobarMiembroDeEquipo(idEquipo, usuario);
        comprobarTareaDeEquipo(idEquipo, idTarea);

        tareaService.borraTarea(idTarea);
        flash.addFlashAttribute("mensaje", "Tarea borrada correctamente");
        return "redirect:/equipos/" + idEquipo;
    }

    // Un miembro se asigna a sí mismo una tarea del equipo que estaba sin asignar
    @PostMapping("/equipos/{id}/tareas/{idTarea}/asignarme")
    public String asignarmeTareaEquipo(@PathVariable(value="id") Long idEquipo,
                                       @PathVariable(value="idTarea") Long idTarea,
                                       RedirectAttributes flash) {
        UsuarioData usuario = comprobarUsuarioLogeado();
        comprobarMiembroDeEquipo(idEquipo, usuario);
        TareaData tarea = comprobarTareaDeEquipo(idEquipo, idTarea);

        try {
            tareaService.modificaTareaEquipo(idTarea, tarea.getTitulo(), usuario.getId());
            flash.addFlashAttribute("mensaje", "Te has asignado la tarea");
        } catch (TareaServiceException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipos/" + idEquipo;
    }
}