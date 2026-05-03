package br.com.desafio.votacao.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.desafio.votacao.cpf.domain.CpfValidator;
import br.com.desafio.votacao.cpf.domain.StatusValidacaoCpf;
import br.com.desafio.votacao.shared.MutableClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class FluxoCompletoIntegracaoTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.of("-03:00");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 5, 1, 12, 0);

    @TestConfiguration
    static class RelogioMutavelConfig {
        @Bean
        @Primary
        public MutableClock relogioTeste() {
            return new MutableClock(INICIO.toInstant(BRASILIA), BRASILIA);
        }

        @Bean
        @Primary
        public CpfValidator cpfValidatorPermissivo() {
            return cpf -> StatusValidacaoCpf.ABLE_TO_VOTE;
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        clock.definir(INICIO.toInstant(BRASILIA));
    }

    @Test
    void fluxoCompleto_criaPautaAbreSessaoVotaApuraAntesEDepoisDoEncerramento() throws Exception {
        long pautaId = criarPauta("Aprovação 2026", "Balanço anual");

        abrirSessao(pautaId, 5).andExpect(status().isCreated());

        votar(pautaId, "11111111111", "SIM").andExpect(status().isCreated());
        votar(pautaId, "22222222222", "SIM").andExpect(status().isCreated());
        votar(pautaId, "33333333333", "NAO").andExpect(status().isCreated());

        votar(pautaId, "11111111111", "NAO").andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/pautas/" + pautaId + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SESSAO_ABERTA"))
                .andExpect(jsonPath("$.resultado").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.totalSim").value(2))
                .andExpect(jsonPath("$.totalNao").value(1))
                .andExpect(jsonPath("$.totalVotos").value(3));

        clock.avancar(Duration.ofMinutes(6));

        mockMvc.perform(get("/api/v1/pautas/" + pautaId + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENCERRADA"))
                .andExpect(jsonPath("$.resultado").value("APROVADA"))
                .andExpect(jsonPath("$.totalVotos").value(3));

        votar(pautaId, "44444444444", "SIM").andExpect(status().isConflict());
    }

    @Test
    void abrirSegundaSessaoNaMesmaPautaRetorna409() throws Exception {
        long pautaId = criarPauta("Pauta única-sessão", null);
        abrirSessao(pautaId, 5).andExpect(status().isCreated());

        abrirSessao(pautaId, 10)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("já possui")));
    }

    @Test
    void votarEmPautaSemSessaoRetorna409() throws Exception {
        long pautaId = criarPauta("Pauta sem sessão", null);

        votar(pautaId, "11111111111", "SIM")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("não possui sessão")));
    }

    @Test
    void apurarPautaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/pautas/99999/resultado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void openApiDocsDisponiveis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API de Votação Cooperativista"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    private long criarPauta(String titulo, String descricao) throws Exception {
        String body = objectMapper.writeValueAsString(new CriarPautaPayload(titulo, descricao));
        String response = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private ResultActions abrirSessao(long pautaId, Integer duracaoMinutos) throws Exception {
        String body = objectMapper.writeValueAsString(new AbrirSessaoPayload(duracaoMinutos));
        return mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/sessoes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions votar(long pautaId, String cpf, String voto) throws Exception {
        String body = """
                {"cpf":"%s","voto":"%s"}
                """.formatted(cpf, voto);
        return mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private record CriarPautaPayload(String titulo, String descricao) {
    }

    private record AbrirSessaoPayload(Integer duracaoMinutos) {
    }
}
