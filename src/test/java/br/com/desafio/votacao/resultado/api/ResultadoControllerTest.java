package br.com.desafio.votacao.resultado.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.resultado.domain.ResultadoApurado;
import br.com.desafio.votacao.resultado.domain.ResultadoVotacao;
import br.com.desafio.votacao.resultado.service.ResultadoService;
import br.com.desafio.votacao.shared.config.ClockConfig;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResultadoController.class)
@Import(ClockConfig.class)
class ResultadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResultadoService resultadoService;

    @Test
    void getRetornaSemSessao() throws Exception {
        given(resultadoService.apurar(1L)).willReturn(
                new ResultadoApurado(1L, EstadoPauta.SEM_SESSAO, 0, 0, 0, ResultadoVotacao.SEM_SESSAO));

        mockMvc.perform(get("/api/v1/pautas/1/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SEM_SESSAO"))
                .andExpect(jsonPath("$.resultado").value("SEM_SESSAO"))
                .andExpect(jsonPath("$.totalVotos").value(0));
    }

    @Test
    void getRetornaEmAndamento() throws Exception {
        given(resultadoService.apurar(1L)).willReturn(
                new ResultadoApurado(1L, EstadoPauta.SESSAO_ABERTA, 3, 2, 5, ResultadoVotacao.EM_ANDAMENTO));

        mockMvc.perform(get("/api/v1/pautas/1/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SESSAO_ABERTA"))
                .andExpect(jsonPath("$.resultado").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.totalSim").value(3))
                .andExpect(jsonPath("$.totalNao").value(2))
                .andExpect(jsonPath("$.totalVotos").value(5));
    }

    @Test
    void getRetornaAprovada() throws Exception {
        given(resultadoService.apurar(1L)).willReturn(
                new ResultadoApurado(1L, EstadoPauta.ENCERRADA, 7, 3, 10, ResultadoVotacao.APROVADA));

        mockMvc.perform(get("/api/v1/pautas/1/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENCERRADA"))
                .andExpect(jsonPath("$.resultado").value("APROVADA"));
    }

    @Test
    void getRetornaRejeitada() throws Exception {
        given(resultadoService.apurar(1L)).willReturn(
                new ResultadoApurado(1L, EstadoPauta.ENCERRADA, 2, 8, 10, ResultadoVotacao.REJEITADA));

        mockMvc.perform(get("/api/v1/pautas/1/resultado"))
                .andExpect(jsonPath("$.resultado").value("REJEITADA"));
    }

    @Test
    void getRetornaEmpate() throws Exception {
        given(resultadoService.apurar(1L)).willReturn(
                new ResultadoApurado(1L, EstadoPauta.ENCERRADA, 5, 5, 10, ResultadoVotacao.EMPATE));

        mockMvc.perform(get("/api/v1/pautas/1/resultado"))
                .andExpect(jsonPath("$.resultado").value("EMPATE"));
    }

    @Test
    void getRetorna404QuandoPautaNaoExiste() throws Exception {
        given(resultadoService.apurar(99L))
                .willThrow(new RecursoNaoEncontradoException("Pauta", 99L));

        mockMvc.perform(get("/api/v1/pautas/99/resultado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
