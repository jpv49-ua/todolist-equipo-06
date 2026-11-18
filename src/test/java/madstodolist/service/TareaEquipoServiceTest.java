package madstodolist.service;

import madstodolist.dto.TareaData;
import madstodolist.dto.UsuarioData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Tests del issue "Añadir tareas al equipo"
@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class TareaEquipoServiceTest {

    @Autowired
    UsuarioService usuarioService;

    @Autowired
    EquipoService equipoService;

    @Autowired
    TareaService tareaService;

    // Registra un usuario y devuelve su id
    Long addUsuarioBD(String email) {

        UsuarioData usuario = new UsuarioData();
        usuario.setEmail(email);
        usuario.setPassword("123");

        return usuarioService.registrar(usuario).getId();
    }

    @Test
    public void nuevaTareaEquipoConResponsableLaAsociaAlEquipoYAlResponsable() {

        Long usuarioId = addUsuarioBD("user@ua");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, usuarioId);

        TareaData tarea = tareaService.nuevaTareaEquipo(equipoId, usuarioId, "Cortar la cebolla");

        assertThat(tarea.getId()).isNotNull();
        assertThat(tarea.getTitulo()).isEqualTo("Cortar la cebolla");
        assertThat(tarea.getEquipoId()).isEqualTo(equipoId);
        assertThat(tarea.getUsuarioId()).isEqualTo(usuarioId);
    }

    @Test
    public void nuevaTareaEquipoSinResponsableLaDejaSinAsignar() {

        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();

        TareaData tarea = tareaService.nuevaTareaEquipo(equipoId, null, "Limpiar los platos");

        //La tarea pertenece al equipo pero no tiene responsable
        assertThat(tarea.getId()).isNotNull();
        assertThat(tarea.getEquipoId()).isEqualTo(equipoId);
        assertThat(tarea.getUsuarioId()).isNull();
    }

    @Test
    public void nuevaTareaEquipoConResponsableQueNoEsMiembroLanzaExcepcion() {

        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        Long ajenoId = addUsuarioBD("ajeno@ua");

        //No se puede asignarle una tarea del equipo
        assertThatThrownBy(() -> tareaService.nuevaTareaEquipo(equipoId, ajenoId, "Sacar la basura"))
                .isInstanceOf(TareaServiceException.class);
    }

    @Test
    public void nuevaTareaEquipoConTituloVacioLanzaExcepcion() {

        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();

        //Lanza excepción cuando le asignas a la tarea un título vacío
        assertThatThrownBy(() -> tareaService.nuevaTareaEquipo(equipoId, null, "   "))
                .isInstanceOf(TareaServiceException.class);
    }

    @Test
    public void nuevaTareaEquipoEnEquipoInexistenteLanzaExcepcion() {

        assertThatThrownBy(() -> tareaService.nuevaTareaEquipo(999L, null, "Tarea fantasma"))
                .isInstanceOf(TareaServiceException.class);
    }

    @Test
    public void allTareasEquipoDevuelveSoloLasTareasDelEquipo() {

        Long usuarioId = addUsuarioBD("user@ua");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, usuarioId);
        tareaService.nuevaTareaEquipo(equipoId, usuarioId, "Cortar la cebolla");
        tareaService.nuevaTareaEquipo(equipoId, null, "Limpiar los platos");
        tareaService.nuevaTareaUsuario(usuarioId, "Renovar el DNI");

        List<TareaData> tareas = tareaService.allTareasEquipo(equipoId);

        assertThat(tareas).hasSize(2);
        assertThat(tareas).extracting(TareaData::getTitulo)
                .containsExactly("Cortar la cebolla", "Limpiar los platos");
    }

    @Test
    public void allTareasUsuarioNoDevuelveLasTareasDeEquipo() {

        Long usuarioId = addUsuarioBD("user@ua");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, usuarioId);
        tareaService.nuevaTareaUsuario(usuarioId, "Renovar el DNI");
        tareaService.nuevaTareaEquipo(equipoId, usuarioId, "Cortar la cebolla");

        List<TareaData> tareas = tareaService.allTareasUsuario(usuarioId);


        //El listado personal solo contiene la tarea personal
        assertThat(tareas).hasSize(1);
        assertThat(tareas.get(0).getTitulo()).isEqualTo("Renovar el DNI");
    }

    @Test
    public void modificaTareaEquipoCambiaTituloYResponsable() {

        Long anaId = addUsuarioBD("ana@ua");
        Long pedroId = addUsuarioBD("pedro@ua");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        equipoService.añadirUsuarioAEquipo(equipoId, pedroId);
        Long tareaId = tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar cebolla").getId();


        TareaData tarea = tareaService.modificaTareaEquipo(tareaId, "Cortar la cebolla", pedroId);

        assertThat(tarea.getTitulo()).isEqualTo("Cortar la cebolla");
        assertThat(tarea.getUsuarioId()).isEqualTo(pedroId);
        assertThat(tarea.getEquipoId()).isEqualTo(equipoId);
    }

    @Test
    public void modificaTareaEquipoPermiteDesasignarLaTarea() {

        Long anaId = addUsuarioBD("ana@ua");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        Long tareaId = tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar la cebolla").getId();

        TareaData tarea = tareaService.modificaTareaEquipo(tareaId, "Cortar la cebolla", null);

        // La tarea sigue en el equipo pero sin responsable
        assertThat(tarea.getUsuarioId()).isNull();
        assertThat(tarea.getEquipoId()).isEqualTo(equipoId);
    }

    @Test
    public void modificaTareaEquipoConResponsableAjenoLanzaExcepcion() {

        //Una tarea de equipo y un usuario que no pertenece al equipo
        Long anaId = addUsuarioBD("ana@ua");
        Long ajenoId = addUsuarioBD("ajeno@ua");
        Long equipoId = equipoService.crearEquipo("Piso compartido").getId();
        equipoService.añadirUsuarioAEquipo(equipoId, anaId);
        Long tareaId = tareaService.nuevaTareaEquipo(equipoId, anaId, "Cortar la cebolla").getId();

        assertThatThrownBy(() -> tareaService.modificaTareaEquipo(tareaId, "Cortar la cebolla", ajenoId))
                .isInstanceOf(TareaServiceException.class);
    }

    @Test
    public void modificaTareaEquipoSobreTareaPersonalLanzaExcepcion() {

        Long anaId = addUsuarioBD("ana@ua");
        Long tareaId = tareaService.nuevaTareaUsuario(anaId, "Renovar el DNI").getId();

        assertThatThrownBy(() -> tareaService.modificaTareaEquipo(tareaId, "Otro título", anaId))
                .isInstanceOf(TareaServiceException.class);
    }
}