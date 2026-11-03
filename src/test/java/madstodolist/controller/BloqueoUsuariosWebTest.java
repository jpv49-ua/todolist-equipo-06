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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class BloqueoUsuariosWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;

    // Método para inicializar los datos de prueba en la BD:
    // un administrador y un usuario normal.
    // Devuelve un mapa con los identificadores de ambos
    Map<String, Long> addUsuariosBD() {
        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setNombre("Administrador");
        admin.setPassword("admin");
        admin.setAdmin(true);
        admin = usuarioService.registrar(admin);

        UsuarioData ana = new UsuarioData();
        ana.setEmail("ana.garcia@gmail.com");
        ana.setNombre("Ana García");
        ana.setPassword("12345678");
        ana = usuarioService.registrar(ana);

        Map<String, Long> ids = new HashMap<>();
        ids.put("adminId", admin.getId());
        ids.put("anaId", ana.getId());
        return ids;
    }

    @Test
    public void listadoUsuariosMuestraBotonBloquear() throws Exception {
        // GIVEN
        // El administrador logeado y un usuario habilitado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("adminId"));

        // WHEN, THEN
        // el listado contiene el botón para bloquear al usuario,
        // pero no para bloquear al administrador
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(allOf(
                        containsString("action=\"/registrados/" + ids.get("anaId") + "/bloquear\""),
                        containsString("Habilitado"),
                        not(containsString("action=\"/registrados/" + ids.get("adminId") + "/bloquear\""))
                )));
    }

    @Test
    public void bloquearUsuarioDesdeListado() throws Exception {
        // GIVEN
        // El administrador logeado y un usuario habilitado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("adminId"));

        // WHEN
        // se pulsa el botón de bloquear del usuario
        this.mockMvc.perform(post("/registrados/" + ids.get("anaId") + "/bloquear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"))
                .andExpect(flash().attribute("mensaje", "Usuario ana.garcia@gmail.com bloqueado correctamente"));

        // THEN
        // el usuario queda bloqueado y el listado muestra el botón para habilitarlo
        assertThat(usuarioService.findById(ids.get("anaId")).isBloqueado()).isTrue();
        this.mockMvc.perform(get("/registrados"))
                .andExpect(content().string(allOf(
                        containsString("action=\"/registrados/" + ids.get("anaId") + "/habilitar\""),
                        containsString("Bloqueado")
                )));
    }

    @Test
    public void habilitarUsuarioDesdeListado() throws Exception {
        // GIVEN
        // El administrador logeado y un usuario bloqueado
        Map<String, Long> ids = addUsuariosBD();
        usuarioService.setBloqueoUsuario(ids.get("anaId"), true);
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("adminId"));

        // WHEN
        // se pulsa el botón de habilitar del usuario
        this.mockMvc.perform(post("/registrados/" + ids.get("anaId") + "/habilitar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));

        // THEN
        // el usuario vuelve a estar habilitado
        assertThat(usuarioService.findById(ids.get("anaId")).isBloqueado()).isFalse();
    }

    @Test
    public void bloquearAdministradorMuestraError() throws Exception {
        // GIVEN
        // El administrador logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("adminId"));

        // WHEN, THEN
        // si se intenta bloquear al administrador se redirige al listado con un error
        // y el administrador no queda bloqueado
        this.mockMvc.perform(post("/registrados/" + ids.get("adminId") + "/bloquear"))
                .andExpect(redirectedUrl("/registrados"))
                .andExpect(flash().attribute("error", "No se puede bloquear al administrador"));
        assertThat(usuarioService.findById(ids.get("adminId")).isBloqueado()).isFalse();
    }

    @Test
    public void bloquearUsuarioInexistenteDevuelveNotFound() throws Exception {
        // GIVEN
        // El administrador logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("adminId"));

        // WHEN, THEN
        // bloquear un usuario que no existe devuelve un error 404
        this.mockMvc.perform(post("/registrados/" + (ids.get("anaId") + 1000L) + "/bloquear"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void bloquearUsuarioSinSerAdministradorDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Un usuario no administrador logeado
        Map<String, Long> ids = addUsuariosBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(ids.get("anaId"));

        // WHEN, THEN
        // la petición devuelve un error 401 y no se bloquea a nadie
        this.mockMvc.perform(post("/registrados/" + ids.get("anaId") + "/bloquear"))
                .andExpect(status().isUnauthorized());
        assertThat(usuarioService.findById(ids.get("anaId")).isBloqueado()).isFalse();
    }

    @Test
    public void loginUsuarioBloqueadoMuestraError() throws Exception {
        // GIVEN
        // Un usuario bloqueado en la BD
        Map<String, Long> ids = addUsuariosBD();
        usuarioService.setBloqueoUsuario(ids.get("anaId"), true);

        // WHEN, THEN
        // al intentar logearse se muestra el mensaje de usuario bloqueado
        this.mockMvc.perform(post("/login")
                        .param("eMail", "ana.garcia@gmail.com")
                        .param("password", "12345678"))
                .andExpect(content().string(containsString("El usuario está bloqueado")));
    }
}
