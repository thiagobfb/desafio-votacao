package br.com.desafio.votacao.voto.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.desafio.votacao.shared.config.ClockConfig;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
import br.com.desafio.votacao.shared.exception.SessaoEncerradaException;
import br.com.desafio.votacao.shared.exception.SessaoNaoAbertaException;
import br.com.desafio.votacao.shared.exception.VotoDuplicadoException;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.domain.Voto;
import br.com.desafio.votacao.voto.service.VotoService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VotoController.class)
@Import(ClockConfig.class)
class VotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VotoService votoService;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);

    private Voto votoComId(long id, long pautaId, String associadoId, Escolha escolha) {
        Voto v = new Voto(pautaId, associadoId, escolha, AGORA);
        v.setId(id);
        return v;
    }

    @Test
    void postRegistraVotoRetorna201() throws Exception {
        given(votoService.registrar(eq(1L), eq("A1"), eq(Escolha.SIM)))
                .willReturn(votoComId(42L, 1L, "A1", Escolha.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"A1","voto":"SIM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1/votos/42"))
                .andExpect(jsonPath("$.votoId").value(42));
    }

    @Test
    void postRejeitaVotoInvalidoCom400ListandoAceitos() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"A1","voto":"TALVEZ"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("voto")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SIM")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NAO")));
    }

    @Test
    void postRejeitaAssociadoVazioCom400() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"","voto":"SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falha de validação"))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("associadoId")));
    }

    @Test
    void postRetorna404QuandoPautaNaoExiste() throws Exception {
        given(votoService.registrar(eq(99L), eq("A1"), eq(Escolha.SIM)))
                .willThrow(new RecursoNaoEncontradoException("Pauta", 99L));

        mockMvc.perform(post("/api/v1/pautas/99/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"A1","voto":"SIM"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void postRetorna409QuandoSessaoNaoAberta() throws Exception {
        given(votoService.registrar(eq(1L), eq("A1"), eq(Escolha.SIM)))
                .willThrow(new SessaoNaoAbertaException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"A1","voto":"SIM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("não possui sessão")));
    }

    @Test
    void postRetorna409QuandoSessaoEncerrada() throws Exception {
        given(votoService.registrar(eq(1L), eq("A1"), eq(Escolha.SIM)))
                .willThrow(new SessaoEncerradaException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"A1","voto":"SIM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("encerrada")));
    }

    @Test
    void postRetorna409QuandoVotoDuplicado() throws Exception {
        given(votoService.registrar(eq(1L), eq("A1"), eq(Escolha.SIM)))
                .willThrow(new VotoDuplicadoException(1L, "A1"));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"associadoId":"A1","voto":"SIM"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já votou")));
    }
}
