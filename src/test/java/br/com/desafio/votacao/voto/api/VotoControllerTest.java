package br.com.desafio.votacao.voto.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.desafio.votacao.shared.config.ClockConfig;
import br.com.desafio.votacao.shared.exception.AssociadoNaoPodeVotarException;
import br.com.desafio.votacao.shared.exception.CpfInvalidoException;
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
    private static final String CPF = "12345678901";

    private Voto votoComId(long id, long pautaId, String cpf, Escolha escolha) {
        Voto v = new Voto(pautaId, cpf, escolha, AGORA);
        v.setId(id);
        return v;
    }

    @Test
    void postRegistraVotoRetorna201() throws Exception {
        given(votoService.registrar(eq(1L), eq(CPF), eq(Escolha.SIM)))
                .willReturn(votoComId(42L, 1L, CPF, Escolha.SIM));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"SIM"}
                                """.formatted(CPF)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1/votos/42"))
                .andExpect(jsonPath("$.votoId").value(42));
    }

    @Test
    void postRejeitaVotoInvalidoCom400ListandoAceitos() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"TALVEZ"}
                                """.formatted(CPF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("voto")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SIM")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NAO")));
    }

    @Test
    void postRejeitaCpfVazioCom400() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"","voto":"SIM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falha de validação"))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("cpf")));
    }

    @Test
    void postRetorna404QuandoPautaNaoExiste() throws Exception {
        given(votoService.registrar(eq(99L), eq(CPF), eq(Escolha.SIM)))
                .willThrow(new RecursoNaoEncontradoException("Pauta", 99L));

        mockMvc.perform(post("/api/v1/pautas/99/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"SIM"}
                                """.formatted(CPF)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void postRetorna404QuandoCpfInvalido() throws Exception {
        given(votoService.registrar(eq(1L), eq("00000000000"), eq(Escolha.SIM)))
                .willThrow(new CpfInvalidoException("00000000000"));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"00000000000","voto":"SIM"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inválido")));
    }

    @Test
    void postRetorna404QuandoAssociadoNaoPodeVotar() throws Exception {
        given(votoService.registrar(eq(1L), eq(CPF), eq(Escolha.SIM)))
                .willThrow(new AssociadoNaoPodeVotarException(CPF));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"SIM"}
                                """.formatted(CPF)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("não está habilitado")));
    }

    @Test
    void postRetorna409QuandoSessaoNaoAberta() throws Exception {
        given(votoService.registrar(eq(1L), eq(CPF), eq(Escolha.SIM)))
                .willThrow(new SessaoNaoAbertaException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"SIM"}
                                """.formatted(CPF)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("não possui sessão")));
    }

    @Test
    void postRetorna409QuandoSessaoEncerrada() throws Exception {
        given(votoService.registrar(eq(1L), eq(CPF), eq(Escolha.SIM)))
                .willThrow(new SessaoEncerradaException(1L));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"SIM"}
                                """.formatted(CPF)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("encerrada")));
    }

    @Test
    void postRetorna409QuandoVotoDuplicado() throws Exception {
        given(votoService.registrar(eq(1L), eq(CPF), eq(Escolha.SIM)))
                .willThrow(new VotoDuplicadoException(1L, CPF));

        mockMvc.perform(post("/api/v1/pautas/1/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cpf":"%s","voto":"SIM"}
                                """.formatted(CPF)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já votou")));
    }
}
