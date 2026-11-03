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

import java.util.HashMap;
import java.util.Map;

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
public class DescripcionUsuarioWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Método para inicializar los datos de prueba en la BD
    // El primer usuario es el administrador.
    // Devuelve un mapa con los identificadores de los dos usuarios añadidos
    Map<String, Long> addUsuariosBD() {
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
        usuario2 = usuarioService.registrar(usuario2);

        Map<String, Long> ids = new HashMap<>();
        ids.put("usuarioId", usuario1.getId());
        ids.put("anaId", usuario2.getId());
        return ids;
    }

    @Test
    public void descripcionUsuarioMuestraDatosDelUsuario() throws Exception {
        // GIVEN
        // Dos usuarios en la BD y el administrador logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // se realiza la petición GET a la descripción del otro usuario,
        // el HTML devuelto contiene sus datos pero no su contraseña.
        this.mockMvc.perform(get("/registrados/" + ids.get("anaId")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Descripción de usuario"),
                        containsString("ana.garcia@gmail.com"),
                        containsString("Ana García"),
                        not(containsString("12345678"))
                )));
    }

    @Test
    public void listadoUsuariosContieneEnlaceADescripcion() throws Exception {
        // GIVEN
        // Dos usuarios en la BD y el administrador logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // el listado de usuarios contiene el enlace a la descripción de cada usuario
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/registrados/" + ids.get("usuarioId") + "\""),
                        containsString("href=\"/registrados/" + ids.get("anaId") + "\"")
                )));
    }

    @Test
    public void descripcionUsuarioInexistenteDevuelveNotFound() throws Exception {
        // GIVEN
        // El administrador logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("usuarioId"));

        // WHEN, THEN
        // la petición de un usuario que no existe devuelve un error 404
        this.mockMvc.perform(get("/registrados/" + (ids.get("anaId") + 1000L)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void descripcionUsuarioSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(get("/registrados/" + ids.get("anaId")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void descripcionUsuarioConUsuarioNoAdministradorDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Un usuario logeado que no es administrador
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("anaId"));

        // WHEN, THEN
        // la petición a la descripción de otro usuario devuelve un error 401
        // con un mensaje de falta de permisos
        this.mockMvc.perform(get("/registrados/" + ids.get("usuarioId")))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("No tienes permisos suficientes para acceder a esta página"));
    }
}
