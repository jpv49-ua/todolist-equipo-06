package madstodolist.controller;

import madstodolist.authentication.ManagerUserSession;
import madstodolist.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class AcercaDeWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    // Moqueamos el managerUserSession para poder moquear el usuario logeado
    @MockBean
    private ManagerUserSession managerUserSession;


    @Test
    public void getAboutDevuelveNombreAplicacion() throws Exception {

        // Moqueamos el método usuarioLogeado para que devuelva null,
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // se realiza la petición GET,
        // el HTML devuelto contiene las descripcion que buscamos.
        String url = "/about";

        this.mockMvc.perform(get(url))
                .andExpect((content().string(
                        containsString("ToDoList")
                )));
    }

    @Test
    public void getAboutDevuelveVersionAplicacion() throws Exception {

        // Moqueamos el método usuarioLogeado para que devuelva null,
        when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        // se realiza la petición GET,
        // el HTML devuelto contiene la versión actual de la aplicación.
        this.mockMvc.perform(get("/about"))
                .andExpect(content().string(containsString("Versión 1.2.0")));
    }
}
