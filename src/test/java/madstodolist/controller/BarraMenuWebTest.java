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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class BarraMenuWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TareaService tareaService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Método para inicializar los datos de prueba en la BD
    // Devuelve un mapa con los identificadores del usuario y de una tarea suya
    Map<String, Long> addUsuarioTareaBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setNombre("Usuario Ejemplo");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);

        TareaData tarea = tareaService.nuevaTareaUsuario(usuario.getId(), "Lavar coche");

        Map<String, Long> ids = new HashMap<>();
        ids.put("usuarioId", usuario.getId());
        ids.put("tareaId", tarea.getId());
        return ids;
    }

    @Test
    public void barraMenuUsuarioLogeadoContieneElementos() throws Exception {
        // GIVEN
        // Un usuario logeado
        Map<String, Long> ids = addUsuarioTareaBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // la barra de menú de la página acerca de contiene los enlaces a ToDoList y Tareas
        // y el desplegable del usuario con Cuenta y Cerrar sesión
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("class=\"navbar navbar-expand-lg"),
                        containsString("href=\"/about\">ToDoList</a>"),
                        containsString("href=\"/usuarios/" + ids.get("usuarioId") + "/tareas\">Tareas</a>"),
                        containsString(">Usuario Ejemplo</a>"),
                        containsString(">Cuenta</a>"),
                        containsString("href=\"/logout\">Cerrar sesión Usuario Ejemplo</a>"),
                        not(containsString("href=\"/login\""))
                )));
    }

    @Test
    public void barraMenuUsuarioNoLogeadoMuestraLoginYRegistro() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la barra de menú de la página acerca de contiene los enlaces a login y registro
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/login\">Login</a>"),
                        containsString("href=\"/registro\">Registro</a>"),
                        not(containsString("href=\"/logout\""))
                )));
    }

    @Test
    public void barraMenuUsuarioSinNombreMuestraEmail() throws Exception {
        // GIVEN
        // Un usuario logeado sin nombre
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("sin.nombre@ua");
        usuario.setPassword("123");
        usuario = usuarioService.registrar(usuario);
        when(managerUserSession.usuarioLogeado()).thenReturn(usuario.getId());

        // WHEN, THEN
        // el desplegable muestra su email
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("Cerrar sesión sin.nombre@ua")));
    }

    @Test
    public void barraMenuEnPaginasDeTareas() throws Exception {
        // GIVEN
        // Un usuario logeado con una tarea
        Map<String, Long> ids = addUsuarioTareaBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // las páginas de listado, nueva tarea y edición de tarea contienen la barra de menú
        String[] urls = {
                "/usuarios/" + ids.get("usuarioId") + "/tareas",
                "/usuarios/" + ids.get("usuarioId") + "/tareas/nueva",
                "/tareas/" + ids.get("tareaId") + "/editar"
        };
        for (String url : urls) {
            this.mockMvc.perform(get(url))
                    .andExpect(content().string(allOf(
                            containsString("class=\"navbar navbar-expand-lg"),
                            containsString("href=\"/logout\">Cerrar sesión Usuario Ejemplo</a>")
                    )));
        }
    }

    @Test
    public void loginYRegistroNoTienenBarraMenu() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // las páginas de login y registro no contienen la barra de menú
        this.mockMvc.perform(get("/login"))
                .andExpect(content().string(not(containsString("class=\"navbar"))));
        this.mockMvc.perform(get("/registro"))
                .andExpect(content().string(not(containsString("class=\"navbar"))));
    }
}
