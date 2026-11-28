package madstodolist.service;

import madstodolist.dto.UsuarioData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarioService;

    // Método para inicializar los datos de prueba en la BD
    // Devuelve el identificador del usuario de la BD
    Long addUsuarioBD() {
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setNombre("Usuario Ejemplo");
        usuario.setPassword("123");
        UsuarioData nuevoUsuario = usuarioService.registrar(usuario);
        return nuevoUsuario.getId();
    }

    @Test
    public void servicioLoginUsuario() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // WHEN
        // intentamos logear un usuario y contraseña correctos
        UsuarioService.LoginStatus loginStatus1 = usuarioService.login("user@ua", "123");

        // intentamos logear un usuario correcto, con una contraseña incorrecta
        UsuarioService.LoginStatus loginStatus2 = usuarioService.login("user@ua", "000");

        // intentamos logear un usuario que no existe,
        UsuarioService.LoginStatus loginStatus3 = usuarioService.login("pepito.perez@gmail.com", "12345678");

        // THEN

        // el valor devuelto por el primer login es LOGIN_OK,
        assertThat(loginStatus1).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);

        // el valor devuelto por el segundo login es ERROR_PASSWORD,
        assertThat(loginStatus2).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // y el valor devuelto por el tercer login es USER_NOT_FOUND.
        assertThat(loginStatus3).isEqualTo(UsuarioService.LoginStatus.USER_NOT_FOUND);
    }

    @Test
    public void servicioRegistroUsuario() {
        // WHEN
        // Registramos un usuario con un e-mail no existente en la base de datos,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba2@gmail.com");
        usuario.setPassword("12345678");

        usuarioService.registrar(usuario);

        // THEN
        // el usuario se añade correctamente al sistema.

        UsuarioData usuarioBaseDatos = usuarioService.findByEmail("usuario.prueba2@gmail.com");
        assertThat(usuarioBaseDatos).isNotNull();
        assertThat(usuarioBaseDatos.getEmail()).isEqualTo("usuario.prueba2@gmail.com");
    }

    @Test
    public void servicioRegistroUsuarioExcepcionConNullPassword() {
        // WHEN, THEN
        // Si intentamos registrar un usuario con un password null,
        // se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }


    @Test
    public void servicioRegistroUsuarioExcepcionConEmailRepetido() {
        // GIVEN
        // Un usuario en la BD

        addUsuarioBD();

        // THEN
        // Si registramos un usuario con un e-mail ya existente en la base de datos,
        // , se produce una excepción de tipo UsuarioServiceException

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@ua");
        usuario.setPassword("12345678");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(usuario);
        });
    }

    @Test
    public void servicioRegistroUsuarioDevuelveUsuarioConId() {

        // WHEN
        // Si registramos en el sistema un usuario con un e-mail no existente en la base de datos,
        // y un password no nulo,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("usuario.prueba@gmail.com");
        usuario.setPassword("12345678");

        UsuarioData usuarioNuevo = usuarioService.registrar(usuario);

        // THEN
        // se actualiza el identificador del usuario

        assertThat(usuarioNuevo.getId()).isNotNull();

        // con el identificador que se ha guardado en la BD.

        UsuarioData usuarioBD = usuarioService.findById(usuarioNuevo.getId());
        assertThat(usuarioBD).isEqualTo(usuarioNuevo);
    }

    @Test
    public void servicioConsultaUsuarioDevuelveUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // recuperamos un usuario usando su e-mail,

        UsuarioData usuario = usuarioService.findByEmail("user@ua");

        // THEN
        // el usuario obtenido es el correcto.

        assertThat(usuario.getId()).isEqualTo(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("user@ua");
        assertThat(usuario.getNombre()).isEqualTo("Usuario Ejemplo");
    }

    @Test
    public void servicioListadoUsuariosDevuelveTodosLosUsuarios() {
        // GIVEN
        // Dos usuarios en la BD

        Long usuarioId = addUsuarioBD();

        UsuarioData usuario2 = new UsuarioData();
        usuario2.setEmail("ana.garcia@gmail.com");
        usuario2.setPassword("12345678");
        usuarioService.registrar(usuario2);

        // WHEN
        // recuperamos el listado de usuarios,

        List<UsuarioData> usuarios = usuarioService.findAllUsuarios();

        // THEN
        // se devuelven los dos usuarios ordenados por id.

        assertThat(usuarios).hasSize(2);
        assertThat(usuarios.get(0).getId()).isEqualTo(usuarioId);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user@ua");
        assertThat(usuarios.get(1).getEmail()).isEqualTo("ana.garcia@gmail.com");
    }

    @Test
    public void servicioConsultaUsuarioPorIdDevuelveUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // recuperamos el usuario usando su identificador,

        UsuarioData usuario = usuarioService.findById(usuarioId);

        // THEN
        // el usuario obtenido es el correcto.

        assertThat(usuario.getId()).isEqualTo(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("user@ua");
        assertThat(usuario.getNombre()).isEqualTo("Usuario Ejemplo");
    }

    @Test
    public void servicioConsultaUsuarioPorIdInexistenteDevuelveNull() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // recuperamos un usuario con un identificador que no existe,

        UsuarioData usuario = usuarioService.findById(usuarioId + 1000L);

        // THEN
        // no se devuelve ningún usuario.

        assertThat(usuario).isNull();
    }

    @Test
    public void servicioRegistroUsuarioAdministrador() {
        // WHEN
        // Registramos un usuario marcado como administrador,

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("admin@ua");
        usuario.setPassword("admin");
        usuario.setAdmin(true);

        usuarioService.registrar(usuario);

        // THEN
        // el usuario se guarda como administrador.

        UsuarioData usuarioBaseDatos = usuarioService.findByEmail("admin@ua");
        assertThat(usuarioBaseDatos.isAdmin()).isTrue();
    }

    @Test
    public void servicioExisteAdministrador() {
        // GIVEN
        // Un usuario no administrador en la BD

        addUsuarioBD();

        // WHEN, THEN
        // no existe administrador hasta que se registra uno.

        assertThat(usuarioService.existeAdministrador()).isFalse();

        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setPassword("admin");
        admin.setAdmin(true);
        usuarioService.registrar(admin);

        assertThat(usuarioService.existeAdministrador()).isTrue();
    }

    @Test
    public void servicioRegistroSegundoAdministradorLanzaExcepcion() {
        // GIVEN
        // Un usuario administrador en la BD

        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setPassword("admin");
        admin.setAdmin(true);
        usuarioService.registrar(admin);

        // WHEN, THEN
        // Si intentamos registrar otro administrador,
        // se produce una excepción de tipo UsuarioServiceException

        UsuarioData otroAdmin = new UsuarioData();
        otroAdmin.setEmail("otro.admin@ua");
        otroAdmin.setPassword("1234");
        otroAdmin.setAdmin(true);

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.registrar(otroAdmin);
        });
        assertThat(usuarioService.findByEmail("otro.admin@ua")).isNull();
    }

    @Test
    public void servicioBloquearYHabilitarUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // bloqueamos al usuario,

        usuarioService.setBloqueoUsuario(usuarioId, true);

        // THEN
        // el usuario queda bloqueado y no puede logearse,

        assertThat(usuarioService.findById(usuarioId).isBloqueado()).isTrue();
        assertThat(usuarioService.login("user@ua", "123")).isEqualTo(UsuarioService.LoginStatus.USER_BLOCKED);

        // y si lo habilitamos de nuevo puede volver a logearse.

        usuarioService.setBloqueoUsuario(usuarioId, false);

        assertThat(usuarioService.findById(usuarioId).isBloqueado()).isFalse();
        assertThat(usuarioService.login("user@ua", "123")).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
    }

    @Test
    public void servicioLoginUsuarioBloqueadoConPasswordIncorrectaDevuelveErrorPassword() {
        // GIVEN
        // Un usuario bloqueado en la BD

        Long usuarioId = addUsuarioBD();
        usuarioService.setBloqueoUsuario(usuarioId, true);

        // WHEN, THEN
        // si la contraseña es incorrecta no se informa de que está bloqueado

        assertThat(usuarioService.login("user@ua", "000")).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);
    }

    @Test
    public void servicioBloquearAdministradorLanzaExcepcion() {
        // GIVEN
        // Un administrador en la BD

        UsuarioData admin = new UsuarioData();
        admin.setEmail("admin@ua");
        admin.setPassword("admin");
        admin.setAdmin(true);
        Long adminId = usuarioService.registrar(admin).getId();

        // WHEN, THEN
        // si intentamos bloquearlo se produce una excepción de tipo UsuarioServiceException

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.setBloqueoUsuario(adminId, true);
        });
        assertThat(usuarioService.findById(adminId).isBloqueado()).isFalse();
    }

    @Test
    public void servicioBloquearUsuarioInexistenteLanzaExcepcion() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN, THEN
        // si intentamos bloquear un usuario que no existe se produce una excepción

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.setBloqueoUsuario(usuarioId + 1000L, true);
        });
    }

    @Test
    public void servicioModificaPerfilUsuario() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // modificamos su email, su nombre y su fecha de nacimiento

        UsuarioData modificado = new UsuarioData();
        modificado.setEmail("nuevo@ua");
        modificado.setNombre("Usuario Modificado");
        modificado.setFechaNacimiento(Date.from(
                LocalDate.of(2000, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        UsuarioData resultado = usuarioService.modificaUsuario(usuarioId, modificado);

        // THEN
        // los datos quedan actualizados en la BD y la contraseña no cambia

        assertThat(resultado.getId()).isEqualTo(usuarioId);
        assertThat(resultado.getEmail()).isEqualTo("nuevo@ua");

        UsuarioData usuarioBD = usuarioService.findById(usuarioId);
        assertThat(usuarioBD.getEmail()).isEqualTo("nuevo@ua");
        assertThat(usuarioBD.getNombre()).isEqualTo("Usuario Modificado");
        assertThat(usuarioBD.getFechaNacimiento()).isNotNull();
        assertThat(usuarioService.login("nuevo@ua", "123")).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
    }

    @Test
    public void servicioModificaPerfilConEmailRepetidoLanzaExcepcion() {
        // GIVEN
        // Dos usuarios en la BD

        Long usuarioId = addUsuarioBD();

        UsuarioData otro = new UsuarioData();
        otro.setEmail("otro@ua");
        otro.setPassword("456");
        usuarioService.registrar(otro);

        // WHEN, THEN
        // si intentamos poner el email del otro usuario se produce una excepción
        // y los datos no cambian

        UsuarioData modificado = new UsuarioData();
        modificado.setEmail("otro@ua");
        modificado.setNombre("Usuario Modificado");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.modificaUsuario(usuarioId, modificado);
        });
        assertThat(usuarioService.findById(usuarioId).getEmail()).isEqualTo("user@ua");
    }

    @Test
    public void servicioModificaPerfilManteniendoSuPropioEmail() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN
        // modificamos solo el nombre, dejando el mismo email

        UsuarioData modificado = new UsuarioData();
        modificado.setEmail("user@ua");
        modificado.setNombre("Otro Nombre");
        usuarioService.modificaUsuario(usuarioId, modificado);

        // THEN
        // el nombre se actualiza sin que el email duplicado dé error

        assertThat(usuarioService.findById(usuarioId).getNombre()).isEqualTo("Otro Nombre");
    }

    @Test
    public void servicioModificaPerfilConEmailVacioLanzaExcepcion() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN, THEN
        // si intentamos dejar el email vacío se produce una excepción

        UsuarioData modificado = new UsuarioData();
        modificado.setEmail("   ");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.modificaUsuario(usuarioId, modificado);
        });
    }

    @Test
    public void servicioModificaPerfilDeUsuarioInexistenteLanzaExcepcion() {
        // GIVEN
        // Un usuario en la BD

        Long usuarioId = addUsuarioBD();

        // WHEN, THEN
        // si intentamos modificar un usuario que no existe se produce una excepción

        UsuarioData modificado = new UsuarioData();
        modificado.setEmail("nuevo@ua");

        Assertions.assertThrows(UsuarioServiceException.class, () -> {
            usuarioService.modificaUsuario(usuarioId + 1000L, modificado);
        });
    }

    @Test
    public void servicioCambiaPasswordUsuario() {
        // GIVEN
        // Un usuario en la BD con la contraseña "123"

        Long usuarioId = addUsuarioBD();

        // WHEN
        // cambiamos su contraseña indicando la actual correcta

        usuarioService.cambiaPassword(usuarioId, "123", "nueva456");

        // THEN
        // se puede hacer login con la nueva contraseña y no con la antigua,

        assertThat(usuarioService.login("user@ua", "nueva456")).isEqualTo(UsuarioService.LoginStatus.LOGIN_OK);
        assertThat(usuarioService.login("user@ua", "123")).isEqualTo(UsuarioService.LoginStatus.ERROR_PASSWORD);

        // y el resto de datos del usuario no cambia.

        UsuarioData usuario = usuarioService.findById(usuarioId);
        assertThat(usuario.getEmail()).isEqualTo("user@ua");
        assertThat(usuario.getNombre()).isEqualTo("Usuario Ejemplo");
        assertThat(usuario.isAdmin()).isFalse();
        assertThat(usuario.isBloqueado()).isFalse();
    }
}
