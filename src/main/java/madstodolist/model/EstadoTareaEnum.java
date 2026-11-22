package madstodolist.model;

public enum EstadoTareaEnum {
    TODO("Por hacer"),
    IN_PROGRESS("En progreso"),
    DONE("Hecha");

    private final String descripcion;

    EstadoTareaEnum(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
