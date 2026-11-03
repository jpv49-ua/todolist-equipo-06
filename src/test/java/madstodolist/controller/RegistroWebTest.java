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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class RegistroWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    @Test
    public void registroLogeaAlUsuarioYLoLlevaASusTareas() throws Exception {
        // GIVEN
        // Ningún usuario en la BD

        // WHEN, THEN
        // al registrarse se redirige a la lista de tareas del nuevo usuario
        // y queda logeado
        this.mockMvc.perform(post("/registro")
                        .param("email", "ana.garcia@gmail.com")
                        .param("password", "12345678"))
                .andExpect(result -> {
                    Long id = usuarioService.findByEmail("ana.garcia@gmail.com").getId();
                    assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/usuarios/" + id + "/tareas");
                    verify(managerUserSession).logearUsuario(id);
                });
    }

    @Test
    public void registroDeAdministradorLoLogeaYLoLlevaAlListadoDeUsuarios() throws Exception {
        // GIVEN
        // Ningún administrador en la BD

        // WHEN, THEN
        // al registrar al administrador se redirige al listado de usuarios y queda logeado
        this.mockMvc.perform(post("/registro")
                        .param("email", "admin@ua")
                        .param("password", "admin")
                        .param("admin", "true"))
                .andExpect(redirectedUrl("/registrados"));
        verify(managerUserSession).logearUsuario(usuarioService.findByEmail("admin@ua").getId());
    }

    @Test
    public void registroFallidoNoLogeaAlUsuario() throws Exception {
        // GIVEN
        // Un usuario ya registrado
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("ana.garcia@gmail.com");
        usuario.setPassword("12345678");
        usuarioService.registrar(usuario);

        // WHEN
        // se intenta registrar otro usuario con el mismo email
        this.mockMvc.perform(post("/registro")
                .param("email", "ana.garcia@gmail.com")
                .param("password", "000"));

        // THEN
        // no se logea a nadie
        verify(managerUserSession, never()).logearUsuario(anyLong());
    }
}
