package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
import madstodolist.service.TareaService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Tests web del issue "Añadir tareas al equipo"
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class TareaEquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EquipoService equipoService;

    @Autowired
    private TareaService tareaService;

    @MockBean
    private ManagerUserSession managerUserSession;

    Long addUsuarioBD(String email, String nombre) {

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setPassword("12345678");
        return usuarioService.registrar(usuario).getId();
    }

    @Test
    public void detalleEquipoMuestraLasTareasDelEquipoAUnMiembro() throws Exception {

        // Un equipo con una tarea asignada y un miembro logeado
        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar la cebolla");
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        //La vista muestra la tarea, su responsable y el botón de nueva tarea
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cortar la cebolla"),
                        containsString("Ana García"),
                        containsString("Nueva tarea"))));
    }

    @Test
    public void detalleEquipoMuestraLasTareasSinAsignar() throws Exception {

        //Un equipo con una tarea sin responsable y un miembro logeado
        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        tareaService.nuevaTareaEquipo(equipoId, null, "Limpiar los platos");
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        //La tarea aparece en el apartado de tareas sin asignar
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Tareas sin asignar"),
                        containsString("Limpiar los platos"),
                        containsString("Cogerla"))));
    }

    @Test
    public void detalleEquipoNoMuestraLasTareasAQuienNoEsMiembro() throws Exception {

        //Un equipo con tareas y un usuario logeado que no es miembro
        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long pedroId = addUsuarioBD("pedro.lopez@gmail.com", "Pedro López");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar la cebolla");
        when(managerUserSession.usuarioLogeado()).thenReturn(pedroId);

        //No ve ni las tareas ni el apartado de sin asignar
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("Cortar la cebolla")),
                        not(containsString("Tareas sin asignar")))));
    }

    @Test
    public void formNuevaTareaEquipoDevuelve401AQuienNoEsMiembro() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long pedroId = addUsuarioBD("pedro.lopez@gmail.com", "Pedro López");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        when(managerUserSession.usuarioLogeado()).thenReturn(pedroId);

        this.mockMvc.perform(get("/equipos/" + equipoId + "/tareas/nueva"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void formNuevaTareaEquipoDevuelve404SiElEquipoNoExiste() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        this.mockMvc.perform(get("/equipos/999/tareas/nueva"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void nuevaTareaEquipoConResponsableLaCreaYRedirige() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long pedroId = addUsuarioBD("pedro.lopez@gmail.com", "Pedro López");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        equipoService.añadirUsuarioAEquipo(equipoId, pedroId);
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);


        //Ana crea una tarea y se la asigna a Pedro
        this.mockMvc.perform(post("/equipos/" + equipoId + "/tareas/nueva")
                        .param("titulo", "Limpiar los platos")
                        .param("usuarioId", pedroId.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipoId));

        //La tarea existe en el equipo con Pedro como responsable
        assertThat(tareaService.allTareasEquipo(equipoId)).hasSize(1);
        assertThat(tareaService.allTareasEquipo(equipoId).get(0).getUsuarioId()).isEqualTo(pedroId);
    }

    @Test
    public void nuevaTareaEquipoSinResponsableLaDejaSinAsignar() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        this.mockMvc.perform(post("/equipos/" + equipoId + "/tareas/nueva")
                        .param("titulo", "Sacar la basura")
                        .param("usuarioId", ""))
                .andExpect(status().is3xxRedirection());

        assertThat(tareaService.allTareasEquipo(equipoId).get(0).getUsuarioId()).isNull();
    }

    @Test
    public void asignarmeTareaEquipoAsignaLaTareaAlUsuarioLogeado() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        Long tareaId = tareaService.nuevaTareaEquipo(equipoId, null, "Sacar la basura").getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        this.mockMvc.perform(post("/equipos/" + equipoId + "/tareas/" + tareaId + "/asignarme"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/" + equipoId));


        assertThat(tareaService.findById(tareaId).getUsuarioId()).isEqualTo(anaId);
    }

    @Test
    public void borrarTareaDeEquipoLaEliminaDeLaBaseDeDatos() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        Long tareaId = tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar la cebolla").getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        this.mockMvc.perform(post("/equipos/" + equipoId + "/tareas/" + tareaId + "/borrar"))
                .andExpect(status().is3xxRedirection());

        // La tarea ya no existe
        assertThat(tareaService.findById(tareaId)).isNull();
        assertThat(tareaService.allTareasEquipo(equipoId)).isEmpty();
    }

    @Test
    public void borrarTareaDeOtroEquipoDevuelve404() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipo1 = equipoService.crearEquipo("Piso compartido").getId();
        Long equipo2 = equipoService.crearEquipo("Trabajo").getId();
        equipoService.añadirUsuarioAEquipo(equipo1, anaId);
        equipoService.añadirUsuarioAEquipo(equipo2, anaId);
        Long tareaId = tareaService.nuevaTareaEquipo(equipo1, anaId, "Cortar la cebolla").getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        // Intentar borrarla desde el equipo 2 da 404
        this.mockMvc.perform(post("/equipos/" + equipo2 + "/tareas/" + tareaId + "/borrar"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void lasTareasDeEquipoNoAparecenEnElListadoPersonal() throws Exception {

        Long anaId = addUsuarioBD("ana.garcia@gmail.com", "Ana García");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        tareaService.nuevaTareaUsuario(anaId, "Renovar el DNI");
        tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar la cebolla");
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        //El listado personal solo muestra la tarea personal
        this.mockMvc.perform(get("/usuarios/" + anaId + "/tareas"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Renovar el DNI"),
                        not(containsString("Cortar la cebolla")))));
    }
}