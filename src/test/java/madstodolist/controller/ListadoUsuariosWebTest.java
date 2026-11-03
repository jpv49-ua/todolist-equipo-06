package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class ListadoUsuariosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Método para inicializar los datos de prueba en la BD
    // El primer usuario es el administrador y se devuelve su identificador
    Long addUsuariosBD() {
        UsuarioData usuario1 = new UsuarioData();
        usuario1.setEmail("user@ua");
        usuario1.setNombre("Usuario Ejemplo");
        usuario1.setPassword("123");
        usuario1.setAdmin(true);
        usuario1 = usuarioService.registrar(usuario1);

        UsuarioData usuario2 = new UsuarioData();
        usuario2.setEmail("ana.garcia@gmail.com");
        usuario2.setNombre("Ana García");
        usuario2.setPassword("12345678");
        usuarioService.registrar(usuario2);

        return usuario1.getId();
    }

    @Test
    public void listadoUsuariosDevuelveEmailsDeUsuarios() throws Exception {
        // GIVEN
        // Dos usuarios en la BD y el administrador logeado
        Long usuarioId = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // se realiza la petición GET al listado de usuarios,
        // el HTML devuelto contiene el email de ambos usuarios.
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(allOf(
                        containsString("Listado de usuarios registrados"),
                        containsString("user@ua"),
                        containsString("ana.garcia@gmail.com")
                )));
    }

    @Test
    public void listadoUsuariosMuestraEnlaceEnMenu() throws Exception {
        // GIVEN
        // El administrador logeado
        Long usuarioId = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // la barra de menú contiene el enlace al listado de usuarios
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("href=\"/registrados\"")));
    }

    @Test
    public void listadoUsuariosSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void listadoUsuariosConUsuarioNoAdministradorDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Un usuario logeado que no es administrador
        addUsuariosBD();
        Long anaId = usuarioService.findByEmail("ana.garcia@gmail.com").getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        // WHEN, THEN
        // la petición devuelve un error 401 con un mensaje de falta de permisos
        this.mockMvc.perform(get("/registrados"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("No tienes permisos suficientes para acceder a esta página"));
    }

    @Test
    public void menuNoMuestraEnlaceUsuariosANoAdministrador() throws Exception {
        // GIVEN
        // Un usuario logeado que no es administrador
        addUsuariosBD();
        Long anaId = usuarioService.findByEmail("ana.garcia@gmail.com").getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        // WHEN, THEN
        // la barra de menú no contiene el enlace al listado de usuarios
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(not(containsString("href=\"/registrados\""))));
    }
}
