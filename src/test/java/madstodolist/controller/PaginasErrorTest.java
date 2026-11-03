package madstodolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

// Usamos un servidor real porque MockMvc no pasa por la resolución
// de las páginas de error (templates/error)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PaginasErrorTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<String> pedirHtml(HttpMethod metodo, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_HTML));
        return restTemplate.exchange(url, metodo, new HttpEntity<>(headers), String.class);
    }

    @Test
    public void paginaNoAutorizadoExplicaQueFaltanPermisosSinMostrarElCodigo() {
        // GIVEN
        // Ningún usuario logeado

        // WHEN
        // se pide una página que requiere estar logeado
        ResponseEntity<String> respuesta = pedirHtml(HttpMethod.GET, "/equipos");

        // THEN
        // se devuelve 401 con una página que explica el problema sin mostrar el código
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody())
                .contains("No tienes permiso para ver esta página")
                .contains("Iniciar sesión")
                .doesNotContain("401");
    }

    @Test
    public void paginaNoEncontradoExplicaQueNoExisteSinMostrarElCodigo() {
        // WHEN
        // se pide una dirección que no existe
        ResponseEntity<String> respuesta = pedirHtml(HttpMethod.GET, "/no-existe");

        // THEN
        // se devuelve 404 con una página que explica el problema sin mostrar el código
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody())
                .contains("No hemos encontrado lo que buscas")
                .doesNotContain("404");
    }

    @Test
    public void paginaGenericaParaOtrosErroresDelCliente() {
        // WHEN
        // se envía una petición con un método no permitido (405)
        // y otra con un identificador mal formado (400)
        ResponseEntity<String> metodoNoPermitido = pedirHtml(HttpMethod.GET, "/equipos/1/unirse");
        ResponseEntity<String> peticionIncorrecta = pedirHtml(HttpMethod.GET, "/usuarios/abc/tareas");

        // THEN
        // ambas muestran la página genérica sin mostrar el código
        assertThat(metodoNoPermitido.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(peticionIncorrecta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        for (ResponseEntity<String> respuesta : new ResponseEntity[]{metodoNoPermitido, peticionIncorrecta}) {
            assertThat(respuesta.getBody())
                    .contains("No hemos podido procesar tu petición")
                    .doesNotContain("405")
                    .doesNotContain("400");
        }
    }
}
