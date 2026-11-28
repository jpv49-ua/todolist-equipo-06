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
public class CambiarPasswordWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    // Registra un usuario con la contraseña indicada y lo deja logeado. Devuelve su id
    private Long usuarioLogeadoBD(String email, String password) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setNombre("Ana García");
        usuario.setPassword(password);
        Long usuarioId = usuarioService.registrar(usuario).getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        return usuarioId;
    }

    @Test
    public void cuentaEnlazaConElFormularioDeCambioDePassword() throws Exception {
        // GIVEN
        // Un usuario logeado

        usuarioLogeadoBD("ana.garcia@gmail.com", "12345678");

        // WHEN, THEN
        // la página de cuenta tiene un botón para cambiar la contraseña

        this.mockMvc.perform(get("/cuenta"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("href=\"/cuenta/contrasena\""),
                        containsString("Cambiar contraseña"))));
    }
}
