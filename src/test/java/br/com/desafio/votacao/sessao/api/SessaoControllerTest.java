package br.com.desafio.votacao.sessao.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import br.com.desafio.votacao.shared.config.ClockConfig;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
import br.com.desafio.votacao.shared.exception.SessaoJaExisteException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessaoController.class)
@Import(ClockConfig.class)
class SessaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessaoService sessaoService;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);

    private Sessao sessaoComId(long id, long pautaId) {
        Sessao s = new Sessao(pautaId, AGORA, AGORA.plusMinutes(5));
        s.setId(id);
        return s;
    }

    @Test
    void postAbreSessaoRetorna201() throws Exception {
        given(sessaoService.abrir(eq(1L), eq(5))).willReturn(sessaoComId(10L, 1L));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1/sessoes/10"))
                .andExpect(jsonPath("$.sessaoId").value(10))
                .andExpect(jsonPath("$.pautaId").value(1));
    }

    @Test
    void postSemBodyUsaDuracaoDefault() throws Exception {
        given(sessaoService.abrir(eq(1L), eq(null))).willReturn(sessaoComId(10L, 1L));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes"))
                .andExpect(status().isCreated());
    }

    @Test
    void postRetorna404QuandoPautaNaoExiste() throws Exception {
        given(sessaoService.abrir(eq(99L), eq(5)))
                .willThrow(new RecursoNaoEncontradoException("Pauta", 99L));

        mockMvc.perform(post("/api/v1/pautas/99/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos":5}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void postRetorna409QuandoJaExisteSessao() throws Exception {
        given(sessaoService.abrir(eq(1L), eq(5)))
                .willThrow(new SessaoJaExisteException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos":5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já possui")));
    }

    @Test
    void postRetorna400QuandoDuracaoInvalida() throws Exception {
        given(sessaoService.abrir(eq(1L), eq(0)))
                .willThrow(new IllegalArgumentException("Duração deve estar entre 1 e 1440 minutos"));

        mockMvc.perform(post("/api/v1/pautas/1/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"duracaoMinutos":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Duração")));
    }
}
