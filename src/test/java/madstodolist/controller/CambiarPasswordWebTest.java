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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    // Devuelve la etiqueta <input ...> del HTML cuyo atributo name es el indicado
    private String inputConNombre(String html, String nombre) {
        Matcher matcher = Pattern.compile("<input[^>]*name=\"" + nombre + "\"[^>]*>").matcher(html);
        assertThat(matcher.find()).as("Existe el campo " + nombre).isTrue();
        return matcher.group();
    }

    @Test
    public void formularioDeCambioDePasswordTieneTresCamposVacios() throws Exception {
        // GIVEN
        // Un usuario logeado con la contraseña "12345678"

        usuarioLogeadoBD("ana.garcia@gmail.com", "12345678");

        // WHEN
        // pedimos el formulario de cambio de contraseña

        String html = this.mockMvc.perform(get("/cuenta/contrasena"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/cuenta/contrasena\"")))
                .andReturn().getResponse().getContentAsString();

        // THEN
        // tiene los tres campos de tipo password y ninguno viene relleno

        for (String nombre : new String[]{"passwordActual", "passwordNueva", "passwordNuevaRepetida"}) {
            String input = inputConNombre(html, nombre);
            assertThat(input).contains("type=\"password\"");
            assertThat(input).doesNotContainPattern("value=\"[^\"]+\"");
        }
        assertThat(html).doesNotContain("12345678");
    }

    @Test
    public void cambiarPasswordCorrectamenteRedirigeACuenta() throws Exception {
        // GIVEN
        // Un usuario logeado con la contraseña "12345678"

        usuarioLogeadoBD("ana.garcia@gmail.com", "12345678");

        // WHEN
        // enviamos el formulario con la contraseña actual correcta
        // y la nueva repetida correctamente,

        this.mockMvc.perform(post("/cuenta/contrasena")
                        .param("passwordActual", "12345678")
                        .param("passwordNueva", "nueva456")
                        .param("passwordNuevaRepetida", "nueva456"))

        // THEN
        // se redirige a la página de cuenta con un mensaje de confirmación

                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cuenta"))
                .andExpect(flash().attribute("mensaje", "Contraseña modificada correctamente"));

        // y se puede hacer login con la nueva contraseña y no con la antigua.

        assertThat(usuarioService.login("ana.garcia@gmail.com", "nueva456"))
                .isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
        assertThat(usuarioService.login("ana.garcia@gmail.com", "12345678"))
                .isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);
    }

    @Test
    public void cambiarPasswordConActualIncorrectaMuestraError() throws Exception {
        // GIVEN
        // Un usuario logeado con la contraseña "12345678"

        usuarioLogeadoBD("ana.garcia@gmail.com", "12345678");

        // WHEN
        // enviamos el formulario con una contraseña actual incorrecta,

        String html = this.mockMvc.perform(post("/cuenta/contrasena")
                        .param("passwordActual", "incorrecta")
                        .param("passwordNueva", "nueva456")
                        .param("passwordNuevaRepetida", "nueva456"))

        // THEN
        // se vuelve al formulario mostrando el error,

                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("alert-danger"),
                        containsString("La contraseña actual no es correcta"),
                        containsString("action=\"/cuenta/contrasena\""))))
                .andReturn().getResponse().getContentAsString();

        // los campos no vienen rellenos con lo enviado

        assertThat(html).doesNotContain("incorrecta").doesNotContain("nueva456");

        // y la contraseña no se modifica.

        assertThat(usuarioService.login("ana.garcia@gmail.com", "12345678"))
                .isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
    }

    @Test
    public void cambiarPasswordConRepeticionQueNoCoincideMuestraError() throws Exception {
        // GIVEN
        // Un usuario logeado con la contraseña "12345678"

        usuarioLogeadoBD("ana.garcia@gmail.com", "12345678");

        // WHEN
        // enviamos el formulario con la actual correcta,
        // pero la nueva y su repetición no coinciden,

        this.mockMvc.perform(post("/cuenta/contrasena")
                        .param("passwordActual", "12345678")
                        .param("passwordNueva", "nueva456")
                        .param("passwordNuevaRepetida", "otra789"))

        // THEN
        // se vuelve al formulario mostrando el error

                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("alert-danger"),
                        containsString("Las contraseñas nuevas no coinciden"),
                        containsString("action=\"/cuenta/contrasena\""))));

        // y la contraseña no se modifica: el login con la antigua sigue funcionando
        // y con la nueva no.

        assertThat(usuarioService.login("ana.garcia@gmail.com", "12345678"))
                .isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
        assertThat(usuarioService.login("ana.garcia@gmail.com", "nueva456"))
                .isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);
    }

    @Test
    public void cambiarPasswordSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado

        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // tanto el formulario como el envío devuelven un error 401

        this.mockMvc.perform(get("/cuenta/contrasena"))
                .andExpect(status().isUnauthorized());

        this.mockMvc.perform(post("/cuenta/contrasena")
                        .param("passwordActual", "12345678")
                        .param("passwordNueva", "nueva456")
                        .param("passwordNuevaRepetida", "nueva456"))
                .andExpect(status().isUnauthorized());
    }
}
