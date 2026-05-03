package br.com.desafio.votacao.pauta.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.service.EstadoPautaResolver;
import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.shared.config.ClockConfig;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PautaController.class)
@Import(ClockConfig.class)
class PautaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PautaService pautaService;

    @MockBean
    private EstadoPautaResolver estadoResolver;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);

    private Pauta pautaComId(long id, String titulo) {
        Pauta p = new Pauta(titulo, "desc", AGORA);
        p.setId(id);
        return p;
    }

    @Test
    void postCriaPautaRetorna201ComLocation() throws Exception {
        given(pautaService.criar("Aprovação", "desc")).willReturn(pautaComId(1L, "Aprovação"));

        String body = """
                {"titulo":"Aprovação","descricao":"desc"}
                """;

        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pautas/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titulo").value("Aprovação"))
                .andExpect(jsonPath("$.estado").value("SEM_SESSAO"));
    }

    @Test
    void postRejeitaTituloVazioCom400() throws Exception {
        String body = """
                {"titulo":"","descricao":"x"}
                """;

        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Falha de validação"))
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("titulo")));
    }

    @Test
    void postRejeitaCorpoMalFormadoCom400() throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido"));
    }

    @Test
    void getPorIdRetorna200ComEstadoCalculado() throws Exception {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L, "X"));
        given(estadoResolver.estadoDe(1L)).willReturn(EstadoPauta.SESSAO_ABERTA);

        mockMvc.perform(get("/api/v1/pautas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("SESSAO_ABERTA"));
    }

    @Test
    void getPorIdRetorna404QuandoNaoExiste() throws Exception {
        given(pautaService.buscarObrigatorio(99L))
                .willThrow(new RecursoNaoEncontradoException("Pauta", 99L));

        mockMvc.perform(get("/api/v1/pautas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Pauta")));
    }

    @Test
    void getListaRetornaPaginaComEstadoPorPauta() throws Exception {
        Pauta p1 = pautaComId(1L, "P1");
        Pauta p2 = pautaComId(2L, "P2");
        Pageable pageable = PageRequest.of(0, 20);
        given(pautaService.listar(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));
        given(estadoResolver.estadoDe(1L)).willReturn(EstadoPauta.SEM_SESSAO);
        given(estadoResolver.estadoDe(2L)).willReturn(EstadoPauta.ENCERRADA);

        mockMvc.perform(get("/api/v1/pautas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].estado").value("SEM_SESSAO"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].estado").value("ENCERRADA"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
