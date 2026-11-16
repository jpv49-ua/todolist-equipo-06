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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class EditarPerfilWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    // Registra un usuario y lo deja logeado. Devuelve su id
    private Long usuarioLogeadoBD(String email, String nombre) {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setPassword("12345678");
        Long usuarioId = usuarioService.registrar(usuario).getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        return usuarioId;
    }

    @Test
    public void cuentaEnlazaConElFormularioDeEdicion() throws Exception {
        // GIVEN
        // Un usuario logeado

        usuarioLogeadoBD("ana.garcia@gmail.com", "Ana García");

        // WHEN, THEN
        // la página de cuenta tiene un enlace al formulario de edición del perfil

        this.mockMvc.perform(get("/cuenta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/cuenta/editar\"")));
    }

    @Test
    public void formularioDeEdicionMuestraLosDatosActuales() throws Exception {
        // GIVEN
        // Un usuario logeado

        usuarioLogeadoBD("ana.garcia@gmail.com", "Ana García");

        // WHEN, THEN
        // el formulario viene rellenado con su email y su nombre

        this.mockMvc.perform(get("/cuenta/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("ana.garcia@gmail.com"),
                        containsString("Ana García"),
                        containsString("action=\"/cuenta/editar\""))));
    }

    @Test
    public void editarPerfilModificaLosDatosDelUsuario() throws Exception {
        // GIVEN
        // Un usuario logeado

        Long usuarioId = usuarioLogeadoBD("ana.garcia@gmail.com", "Ana García");

        // WHEN
        // enviamos el formulario con los datos modificados

        this.mockMvc.perform(post("/cuenta/editar")
                        .param("email", "ana.nueva@gmail.com")
                        .param("nombre", "Ana Nueva")
                        .param("fechaNacimiento", "15-01-2000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cuenta"));

        // THEN
        // los datos quedan modificados en la base de datos

        UsuarioData usuario = usuarioService.findById(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("ana.nueva@gmail.com");
        assertThat(usuario.getNombre()).isEqualTo("Ana Nueva");
        assertThat(usuario.getFechaNacimiento()).isNotNull();
    }

    @Test
    public void editarPerfilConEmailRepetidoMuestraError() throws Exception {
        // GIVEN
        // Dos usuarios registrados, uno de ellos logeado

        UsuarioData otro = new UsuarioData();
        otro.setEmail("pedro@gmail.com");
        otro.setPassword("12345678");
        usuarioService.registrar(otro);

        Long usuarioId = usuarioLogeadoBD("ana.garcia@gmail.com", "Ana García");

        // WHEN
        // intentamos usar el email del otro usuario

        this.mockMvc.perform(post("/cuenta/editar")
                        .param("email", "pedro@gmail.com")
                        .param("nombre", "Ana García"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ya está registrado")));

        // THEN
        // el email no se modifica

        assertThat(usuarioService.findById(usuarioId).getEmail()).isEqualTo("ana.garcia@gmail.com");
    }

    @Test
    public void editarPerfilConEmailNoValidoMuestraError() throws Exception {
        // GIVEN
        // Un usuario logeado

        Long usuarioId = usuarioLogeadoBD("ana.garcia@gmail.com", "Ana García");

        // WHEN
        // enviamos un email con formato incorrecto

        this.mockMvc.perform(post("/cuenta/editar")
                        .param("email", "esto-no-es-un-email")
                        .param("nombre", "Ana García"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alert-danger")));

        // THEN
        // el email no se modifica

        assertThat(usuarioService.findById(usuarioId).getEmail()).isEqualTo("ana.garcia@gmail.com");
    }

    @Test
    public void editarPerfilSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado

        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // tanto el formulario como el envío devuelven un error 401

        this.mockMvc.perform(get("/cuenta/editar"))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(post("/cuenta/editar")
                        .param("email", "ana.garcia@gmail.com"))
                .andExpect(status().isUnauthorized());
    }
}
