package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.TareaData;
import madstodolist.dto.UsuarioData;
import madstodolist.service.TareaService;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class ComentarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TareaService tareaService;

    @Autowired
    private UsuarioService usuarioService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Método para inicializar los datos de prueba en la BD
    // Devuelve un mapa con los identificadores del usuario y de su tarea, que tiene un comentario
    Map<String, Long> addUsuarioTareaComentarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        TareaData tarea = tareaService.nuevaTareaUsuario(usuario.getId(), "Lavar coche");
        tareaService.guardaComentario(tarea.getId(), "Comprar jabón");

        Map<String, Long> ids = new HashMap<>();
        ids.put("usuarioId", usuario.getId());
        ids.put("tareaId", tarea.getId());
        return ids;
    }

    @Test
    public void detalleTareaMuestraComentarioYFormulario() throws Exception {
        // GIVEN
        // Una tarea con comentario y su dueño logeado,

        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // la página de detalle muestra el título, el comentario
        // y el formulario para modificarlo.

        String url = "/tareas/" + ids.get("tareaId");
        String urlAction = "action=\"/tareas/" + ids.get("tareaId") + "/comentario\"";

        this.mockMvc.perform(get(url))
                .andExpect(content().string(allOf(
                        containsString("Lavar coche"),
                        containsString("Comprar jabón"),
                        containsString(urlAction)
                )));
    }

    @Test
    public void listadoTareasMuestraElComentarioJuntoALaTarea() throws Exception {
        // GIVEN
        // Una tarea con comentario y su dueño logeado,

        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // el listado de tareas muestra la columna de comentario con el comentario de la tarea.

        this.mockMvc.perform(get("/usuarios/" + ids.get("usuarioId") + "/tareas"))
                .andExpect(content().string(allOf(
                        containsString("<th>Comentario</th>"),
                        containsString("Lavar coche"),
                        containsString("Comprar jabón")
                )));
    }

    @Test
    public void detalleTareaDeOtroUsuarioDevuelve401() throws Exception {
        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId") + 1);

        this.mockMvc.perform(get("/tareas/" + ids.get("tareaId")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void detalleTareaInexistenteDevuelve404() throws Exception {
        this.mockMvc.perform(get("/tareas/1000"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void postComentarioSustituyeElComentarioAnterior() throws Exception {
        // GIVEN
        // Una tarea con comentario y su dueño logeado,

        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // al enviar un nuevo comentario se redirige al listado de tareas,

        String urlPost = "/tareas/" + ids.get("tareaId") + "/comentario";
        String urlRedirect = "/usuarios/" + ids.get("usuarioId") + "/tareas";

        this.mockMvc.perform(post(urlPost)
                        .param("comentario", "Ir al túnel de lavado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(urlRedirect));

        // y el listado muestra el nuevo comentario en lugar del anterior.

        this.mockMvc.perform(get(urlRedirect))
                .andExpect(content().string(allOf(
                        containsString("Ir al túnel de lavado"),
                        not(containsString("Comprar jabón"))
                )));
    }

    @Test
    public void postComentarioVacioVuelveAlDetalleSinCambiarElComentario() throws Exception {
        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        this.mockMvc.perform(post("/tareas/" + ids.get("tareaId") + "/comentario")
                        .param("comentario", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tareas/" + ids.get("tareaId")));

        assertThat(tareaService.findById(ids.get("tareaId")).getComentario()).isEqualTo("Comprar jabón");
    }

    @Test
    public void listadoMuestraBotonesDelComentarioSoloSiHayComentario() throws Exception {
        // GIVEN
        // Un usuario con una tarea con comentario y otra sin comentario,

        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        TareaData sinComentario = tareaService.nuevaTareaUsuario(ids.get("usuarioId"), "Renovar DNI");
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // el listado tiene el botón para borrar el comentario de la primera
        // y el botón para añadir comentario en la segunda.

        this.mockMvc.perform(get("/usuarios/" + ids.get("usuarioId") + "/tareas"))
                .andExpect(content().string(allOf(
                        // Thymeleaf escapa las comillas simples del onclick como &#39;
                        containsString("del(&#39;/tareas/" + ids.get("tareaId") + "/comentario&#39;"),
                        not(containsString("del(&#39;/tareas/" + sinComentario.getId() + "/comentario&#39;")),
                        containsString("añadir comentario")
                )));
    }

    @Test
    public void postComentarioDeOtroUsuarioDevuelve401() throws Exception {
        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId") + 1);

        this.mockMvc.perform(post("/tareas/" + ids.get("tareaId") + "/comentario")
                        .param("comentario", "Comentario ajeno"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteComentarioDevuelveOKyBorraComentario() throws Exception {
        // GIVEN
        // Una tarea con comentario y su dueño logeado,

        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // la petición DELETE del comentario devuelve OK,

        this.mockMvc.perform(delete("/tareas/" + ids.get("tareaId") + "/comentario"))
                .andExpect(status().isOk());

        // y la tarea sigue existiendo pero sin comentario.

        TareaData tarea = tareaService.findById(ids.get("tareaId"));
        assertThat(tarea).isNotNull();
        assertThat(tarea.getComentario()).isNull();
    }

    @Test
    public void deleteComentarioDeOtroUsuarioDevuelve401() throws Exception {
        Map<String, Long> ids = addUsuarioTareaComentarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId") + 1);

        this.mockMvc.perform(delete("/tareas/" + ids.get("tareaId") + "/comentario"))
                .andExpect(status().isUnauthorized());
    }
}
