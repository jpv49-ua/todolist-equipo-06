package madstodolist.repository;

import madstodolist.model.Tarea;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface TareaRepository extends CrudRepository<Tarea, Long> {

    List<Tarea> findByEquipoIdOrderByIdAsc(Long equipoId);
    List<Tarea> findByUsuarioIdAndEquipoIsNullOrderByIdAsc(Long usuarioId);
}
