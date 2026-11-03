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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class CuentaWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    @Test
    public void cuentaMuestraDatosDelUsuarioLogeado() throws Exception {
        // GIVEN
        // Un usuario logeado
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("ana.garcia@gmail.com");
        usuario.setNombre("Ana García");
        usuario.setPassword("12345678");
        Long usuarioId = usuarioService.registrar(usuario).getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // la página de cuenta muestra su email y su nombre
        this.mockMvc.perform(get("/cuenta"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("ana.garcia@gmail.com"),
                        containsString("Ana García"))));
    }

    @Test
    public void menuEnlazaConLaCuenta() throws Exception {
        // GIVEN
        // Un usuario logeado
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("ana.garcia@gmail.com");
        usuario.setPassword("12345678");
        Long usuarioId = usuarioService.registrar(usuario).getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // la opción Cuenta del menú apunta a /cuenta
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("href=\"/cuenta\"")));
    }

    @Test
    public void cuentaSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(get("/cuenta"))
                .andExpect(status().isUnauthorized());
    }
}
