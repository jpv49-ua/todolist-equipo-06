package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.dto.UsuarioData;
import madstodolist.service.EquipoService;
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
import static org.assertj.core.api.Assertions.assertThat;
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
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EquipoService equipoService;

    @MockBean
    private ManagerUserSession managerUserSession;

    // Crea un usuario no administrador y devuelve su identificador
    Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("ana.garcia@gmail.com");
        usuario.setNombre("Ana García");
        usuario.setPassword("12345678");
        return usuarioService.registrar(usuario).getId();
    }

    @Test
    public void listadoEquiposMuestraNombresOrdenados() throws Exception {
        // GIVEN
        // Dos equipos en la BD y un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        equipoService.crearEquipo("Proyecto P1");
        equipoService.crearEquipo("Proyecto A1");

        // WHEN, THEN
        // el HTML contiene ambos nombres, primero el de orden alfabético menor
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Listado de equipos"),
                        containsString("Proyecto A1"),
                        containsString("Proyecto P1"))))
                .andExpect(result -> {
                    String html = result.getResponse().getContentAsString();
                    org.junit.jupiter.api.Assertions.assertTrue(
                            html.indexOf("Proyecto A1") < html.indexOf("Proyecto P1"));
                });
    }

    @Test
    public void listadoEquiposSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void detalleEquipoMuestraNombreYMiembros() throws Exception {
        // GIVEN
        // Un equipo con un miembro y otro usuario que no pertenece a él
        Long anaId = addUsuarioBD();
        UsuarioData otro = new UsuarioData();
        otro.setEmail("otro@ua");
        otro.setPassword("123");
        usuarioService.registrar(otro);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        // WHEN, THEN
        // el HTML contiene el nombre del equipo y solo el email de su miembro
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Proyecto P1"),
                        containsString("ana.garcia@gmail.com"),
                        not(containsString("otro@ua")))));
    }

    @Test
    public void detalleEquipoInexistenteDevuelveNotFound() throws Exception {
        // GIVEN
        // Un usuario logeado y ningún equipo con el id solicitado
        Long anaId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        // WHEN, THEN
        // la petición devuelve un error 404
        this.mockMvc.perform(get("/equipos/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void detalleEquipoSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Un equipo existente y ningún usuario logeado
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void menuMuestraEnlaceEquipos() throws Exception {
        // GIVEN
        // Un usuario logeado
        Long anaId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);

        // WHEN, THEN
        // la barra de menú contiene el enlace al listado de equipos
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("href=\"/equipos\"")));
    }

    @Test
    public void listadoEquiposEnlazaNombreYBotonAlDetalle() throws Exception {
        // GIVEN
        // Un equipo en la BD y un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // el nombre del equipo es un enlace y además hay un botón "Ver miembros"
        this.mockMvc.perform(get("/equipos"))
                .andExpect(content().string(allOf(
                        containsString("href=\"/equipos/" + equipoId + "\">Proyecto P1</a>"),
                        containsString("Ver miembros"))));
    }

    @Test
    public void detalleEquipoSinMiembrosMuestraMensaje() throws Exception {
        // GIVEN
        // Un equipo sin miembros y un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // la vista indica que el equipo no tiene miembros
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(content().string(containsString("Este equipo todavía no tiene miembros")));
    }

    @Test
    public void formularioNuevoEquipoMuestraCampoNombre() throws Exception {
        // GIVEN
        // Un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // el formulario contiene el campo del nombre
        this.mockMvc.perform(get("/equipos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Nuevo equipo"),
                        containsString("name=\"nombre\""))));
    }

    @Test
    public void crearEquipoLoCreaYApuntaAlCreador() throws Exception {
        // GIVEN
        // Un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // al crear un equipo se redirige al listado y el usuario es miembro
        this.mockMvc.perform(post("/equipos/nuevo").param("nombre", "Proyecto P1"))
                .andExpect(redirectedUrl("/equipos"));
        assertThat(equipoService.equiposUsuario(usuarioId)).hasSize(1);
        assertThat(equipoService.equiposUsuario(usuarioId).get(0).getNombre()).isEqualTo("Proyecto P1");
    }

    @Test
    public void crearEquipoConNombreVacioMuestraError() throws Exception {
        // GIVEN
        // Un usuario logeado
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);

        // WHEN, THEN
        // se vuelve al formulario con un error y no se crea ningún equipo
        this.mockMvc.perform(post("/equipos/nuevo").param("nombre", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El nombre del equipo no puede estar vacío")));
        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @Test
    public void crearEquipoSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(post("/equipos/nuevo").param("nombre", "Proyecto P1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void unirseAEquipoAñadeAlUsuario() throws Exception {
        // GIVEN
        // Un usuario logeado y un equipo al que no pertenece
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // al unirse se redirige al detalle y el usuario es miembro
        this.mockMvc.perform(post("/equipos/" + equipoId + "/unirse"))
                .andExpect(redirectedUrl("/equipos/" + equipoId));
        assertThat(equipoService.usuariosEquipo(equipoId)).hasSize(1);
    }

    @Test
    public void unirseAEquipoAlQueYaPerteneceNoDuplica() throws Exception {
        // GIVEN
        // Un usuario logeado que ya es miembro de un equipo
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, usuarioId);

        // WHEN, THEN
        // se redirige al detalle con un error y sigue habiendo un solo miembro
        this.mockMvc.perform(post("/equipos/" + equipoId + "/unirse"))
                .andExpect(redirectedUrl("/equipos/" + equipoId))
                .andExpect(flash().attributeExists("error"));
        assertThat(equipoService.usuariosEquipo(equipoId)).hasSize(1);
    }

    @Test
    public void salirDeEquipoEliminaAlUsuario() throws Exception {
        // GIVEN
        // Un usuario logeado que es miembro de un equipo
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, usuarioId);

        // WHEN, THEN
        // al salir se redirige al detalle y el equipo queda sin miembros
        this.mockMvc.perform(post("/equipos/" + equipoId + "/salir"))
                .andExpect(redirectedUrl("/equipos/" + equipoId));
        assertThat(equipoService.usuariosEquipo(equipoId)).isEmpty();
    }

    @Test
    public void salirDeEquipoAlQueNoPerteneceMuestraError() throws Exception {
        // GIVEN
        // Un usuario logeado y un equipo al que no pertenece
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // se redirige al detalle con un error
        this.mockMvc.perform(post("/equipos/" + equipoId + "/salir"))
                .andExpect(redirectedUrl("/equipos/" + equipoId))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    public void detalleMuestraBotonSegunPertenencia() throws Exception {
        // GIVEN
        // Un usuario logeado y un equipo al que no pertenece
        Long usuarioId = addUsuarioBD();
        when(managerUserSession.usuarioLogeado()).thenReturn(usuarioId);
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // se ofrece unirse; tras unirse se ofrece salir
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(content().string(allOf(
                        containsString("Unirme al equipo"),
                        not(containsString("Salir del equipo")))));
        equipoService.añadirUsuarioAEquipo(equipoId, usuarioId);
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(content().string(allOf(
                        containsString("Salir del equipo"),
                        not(containsString("Unirme al equipo")))));
    }

    // Crea un usuario administrador y devuelve su identificador
    Long addAdminBD() {
        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setNombre("Admin");
        admin.setPassword("12345678");
        admin.setAdmin(true);
        return usuarioService.registrar(admin).getId();
    }

    @Test
    public void formularioEditarEquipoMuestraNombreActual() throws Exception {
        // GIVEN
        // Un administrador logeado y un equipo
        when(managerUserSession.usuarioLogeado()).thenReturn(addAdminBD());
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // el formulario contiene el nombre actual del equipo
        this.mockMvc.perform(get("/equipos/" + equipoId + "/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Editar equipo"),
                        containsString("value=\"Proyecto P1\""))));
    }

    @Test
    public void adminPuedeModificarElNombreDeUnEquipo() throws Exception {
        // GIVEN
        // Un administrador logeado y un equipo
        when(managerUserSession.usuarioLogeado()).thenReturn(addAdminBD());
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // se redirige al listado y el equipo tiene el nuevo nombre
        this.mockMvc.perform(post("/equipos/" + equipoId + "/editar").param("nombre", "Proyecto P2"))
                .andExpect(redirectedUrl("/equipos"));
        assertThat(equipoService.recuperarEquipo(equipoId).getNombre()).isEqualTo("Proyecto P2");
    }

    @Test
    public void modificarEquipoConNombreVacioMuestraError() throws Exception {
        // GIVEN
        // Un administrador logeado y un equipo
        when(managerUserSession.usuarioLogeado()).thenReturn(addAdminBD());
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // se vuelve al formulario con un error y el nombre no cambia
        this.mockMvc.perform(post("/equipos/" + equipoId + "/editar").param("nombre", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El nombre del equipo no puede estar vacío")));
        assertThat(equipoService.recuperarEquipo(equipoId).getNombre()).isEqualTo("Proyecto P1");
    }

    @Test
    public void adminPuedeEliminarUnEquipo() throws Exception {
        // GIVEN
        // Un administrador logeado y un equipo
        when(managerUserSession.usuarioLogeado()).thenReturn(addAdminBD());
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // se redirige al listado y el equipo ya no existe
        this.mockMvc.perform(post("/equipos/" + equipoId + "/eliminar"))
                .andExpect(redirectedUrl("/equipos"));
        assertThat(equipoService.findAllOrdenadoPorNombre()).isEmpty();
    }

    @Test
    public void editarOEliminarEquipoInexistenteDevuelveNotFound() throws Exception {
        // GIVEN
        // Un administrador logeado y ningún equipo
        when(managerUserSession.usuarioLogeado()).thenReturn(addAdminBD());

        // WHEN, THEN
        // las tres peticiones devuelven un error 404
        this.mockMvc.perform(get("/equipos/999/editar")).andExpect(status().isNotFound());
        this.mockMvc.perform(post("/equipos/999/editar").param("nombre", "X")).andExpect(status().isNotFound());
        this.mockMvc.perform(post("/equipos/999/eliminar")).andExpect(status().isNotFound());
    }

    @Test
    public void usuarioNoAdminNoPuedeEditarNiEliminarEquipos() throws Exception {
        // GIVEN
        // Un usuario no administrador logeado y un equipo
        when(managerUserSession.usuarioLogeado()).thenReturn(addUsuarioBD());
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();

        // WHEN, THEN
        // las peticiones devuelven 401 y el equipo no cambia
        this.mockMvc.perform(get("/equipos/" + equipoId + "/editar")).andExpect(status().isUnauthorized());
        this.mockMvc.perform(post("/equipos/" + equipoId + "/editar").param("nombre", "X"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(post("/equipos/" + equipoId + "/eliminar")).andExpect(status().isUnauthorized());
        assertThat(equipoService.recuperarEquipo(equipoId).getNombre()).isEqualTo("Proyecto P1");
    }

    @Test
    public void editarEquipoSinLogearDevuelveNoAutorizado() throws Exception {
        // GIVEN
        // Ningún usuario logeado
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // la petición devuelve un error 401
        this.mockMvc.perform(post("/equipos/1/eliminar")).andExpect(status().isUnauthorized());
    }

    @Test
    public void botonesDeAdminSoloVisiblesParaElAdministrador() throws Exception {
        // GIVEN
        // Un equipo en la BD
        Long equipoId = equipoService.crearEquipo("Proyecto P1").getId();
        Long adminId = addAdminBD();
        Long anaId = addUsuarioBD();

        // WHEN, THEN
        // el administrador ve Editar y Eliminar en el listado y en el detalle; un usuario normal no
        when(managerUserSession.usuarioLogeado()).thenReturn(adminId);
        this.mockMvc.perform(get("/equipos"))
                .andExpect(content().string(allOf(containsString("Editar"), containsString("Eliminar"))));
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(content().string(allOf(containsString("Editar equipo"), containsString("Eliminar equipo"))));

        when(managerUserSession.usuarioLogeado()).thenReturn(anaId);
        this.mockMvc.perform(get("/equipos"))
                .andExpect(content().string(allOf(not(containsString("Editar")), not(containsString("Eliminar")))));
        this.mockMvc.perform(get("/equipos/" + equipoId))
                .andExpect(content().string(allOf(not(containsString("Editar equipo")), not(containsString("Eliminar equipo")))));
    }
}
