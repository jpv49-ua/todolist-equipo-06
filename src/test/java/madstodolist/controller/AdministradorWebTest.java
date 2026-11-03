package madstodolist.controller;

import madstodolist.dto.UsuarioData;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/clean-db.sql")
public class AdministradorWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    // Método para añadir un usuario administrador a la BD
    // Devuelve el identificador del administrador
    Long addAdministradorBD() {
        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setNombre("Administrador");
        admin.setPassword("admin");
        admin.setAdmin(true);
        return usuarioService.registrar(admin).getId();
    }

    @Test
    public void formRegistroMuestraCheckboxAdminSiNoHayAdministrador() throws Exception {
        // GIVEN
        // Ningún administrador en la BD

        // WHEN, THEN
        // el formulario de registro contiene el checkbox de administrador
        this.mockMvc.perform(get("/registro"))
                .andExpect(content().string(containsString("Registrar como administrador")));
    }

    @Test
    public void formRegistroNoMuestraCheckboxAdminSiYaHayAdministrador() throws Exception {
        // GIVEN
        // Un administrador en la BD
        addAdministradorBD();

        // WHEN, THEN
        // el formulario de registro no contiene el checkbox de administrador
        this.mockMvc.perform(get("/registro"))
                .andExpect(content().string(not(containsString("Registrar como administrador"))));
    }

    @Test
    public void registroConCheckboxAdminCreaAdministrador() throws Exception {
        // GIVEN
        // Ningún administrador en la BD

        // WHEN
        // se envía el formulario de registro marcando el checkbox de administrador
        this.mockMvc.perform(post("/registro")
                        .param("email", "admin@ua")
                        .param("password", "admin")
                        .param("admin", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));

        // THEN
        // el usuario queda registrado como administrador
        assertThat(usuarioService.findByEmail("admin@ua").isAdmin()).isTrue();
    }

    @Test
    public void registroSegundoAdministradorMuestraError() throws Exception {
        // GIVEN
        // Un administrador en la BD
        addAdministradorBD();

        // WHEN
        // se intenta registrar otro usuario como administrador
        this.mockMvc.perform(post("/registro")
                        .param("email", "otro.admin@ua")
                        .param("password", "1234")
                        .param("admin", "true"))
                .andExpect(content().string(containsString("Ya existe un usuario administrador")));

        // THEN
        // el usuario no se registra
        assertThat(usuarioService.findByEmail("otro.admin@ua")).isNull();
    }

    @Test
    public void loginAdministradorRedirigeAListadoUsuarios() throws Exception {
        // GIVEN
        // Un administrador en la BD
        addAdministradorBD();

        // WHEN, THEN
        // al hacer login con el administrador se redirige al listado de usuarios
        this.mockMvc.perform(post("/login")
                        .param("eMail", "admin@ua")
                        .param("password", "admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrados"));
    }
}
