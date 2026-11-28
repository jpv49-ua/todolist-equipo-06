package madstodolist.dto;

// Clase de datos para el formulario de cambio de contraseña del usuario
public class PasswordData {

    private String passwordActual;
    private String passwordNueva;
    private String passwordNuevaRepetida;

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public String getPasswordNueva() {
        return passwordNueva;
    }

    public void setPasswordNueva(String passwordNueva) {
        this.passwordNueva = passwordNueva;
    }

    public String getPasswordNuevaRepetida() {
        return passwordNuevaRepetida;
    }

    public void setPasswordNuevaRepetida(String passwordNuevaRepetida) {
        this.passwordNuevaRepetida = passwordNuevaRepetida;
    }
}
