package it.unisa.resolveIt.common;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @MethodSource("forbiddenRoutes")
    void deniesRoutesOutsideUsersRole(String authority, HttpMethod method, String path) throws Exception {
        MockHttpServletRequestBuilder request = method == HttpMethod.POST
                ? post(path).with(csrf())
                : get(path);

        mockMvc.perform(request.with(user("test").authorities(new SimpleGrantedAuthority(authority))))
                .andExpect(status().is3xxRedirection());
    }

    private static Stream<Arguments> forbiddenRoutes() {
        return Stream.of(
                Arguments.of("CLIENTE", HttpMethod.GET, "/gestore"),
                Arguments.of("CLIENTE", HttpMethod.GET, "/ticket/operatore-home"),
                Arguments.of("CLIENTE", HttpMethod.POST, "/account/removeCliente"),
                Arguments.of("OPERATORE", HttpMethod.GET, "/gestore"),
                Arguments.of("OPERATORE", HttpMethod.GET, "/ticket/home"),
                Arguments.of("OPERATORE", HttpMethod.POST, "/categoria/disableCategoria"),
                Arguments.of("GESTORE", HttpMethod.GET, "/ticket/home"),
                Arguments.of("GESTORE", HttpMethod.GET, "/ticket/operatore-home"),
                Arguments.of("GESTORE", HttpMethod.GET, "/my-profile")
        );
    }
}
