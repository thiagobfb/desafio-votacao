package br.com.desafio.votacao.resultado.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.resultado.domain.ResultadoApurado;
import br.com.desafio.votacao.resultado.domain.ResultadoVotacao;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.repository.VotoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultadoServiceTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.of("-03:00");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);
    private final Clock clock = Clock.fixed(AGORA.toInstant(BRASILIA), BRASILIA);

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoService sessaoService;

    @Mock
    private VotoRepository votoRepository;

    private ResultadoService service;

    @BeforeEach
    void setup() {
        service = new ResultadoService(pautaService, sessaoService, votoRepository, clock);
    }

    private Pauta pautaComId(long id) {
        Pauta p = new Pauta("X", null, AGORA);
        p.setId(id);
        return p;
    }

    @Test
    void retornaSemSessaoQuandoPautaNaoTemSessao() {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.empty());
        given(votoRepository.countByPautaIdAndEscolha(1L, Escolha.SIM)).willReturn(0L);
        given(votoRepository.countByPautaIdAndEscolha(1L, Escolha.NAO)).willReturn(0L);

        ResultadoApurado r = service.apurar(1L);

        assertThat(r.estado()).isEqualTo(EstadoPauta.SEM_SESSAO);
        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.SEM_SESSAO);
        assertThat(r.totalVotos()).isZero();
    }

    @Test
    void retornaEmAndamentoComContagemParcialQuandoSessaoAberta() {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L));
        Sessao aberta = new Sessao(1L, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(aberta));
        given(votoRepository.countByPautaIdAndEscolha(1L, Escolha.SIM)).willReturn(3L);
        given(votoRepository.countByPautaIdAndEscolha(1L, Escolha.NAO)).willReturn(2L);

        ResultadoApurado r = service.apurar(1L);

        assertThat(r.estado()).isEqualTo(EstadoPauta.SESSAO_ABERTA);
        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.EM_ANDAMENTO);
        assertThat(r.totalSim()).isEqualTo(3);
        assertThat(r.totalNao()).isEqualTo(2);
        assertThat(r.totalVotos()).isEqualTo(5);
    }

    @Test
    void retornaAprovadaComEncerradaESimMaior() {
        configurarSessaoEncerradaCom(7L, 3L);

        ResultadoApurado r = service.apurar(1L);

        assertThat(r.estado()).isEqualTo(EstadoPauta.ENCERRADA);
        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.APROVADA);
        assertThat(r.totalVotos()).isEqualTo(10);
    }

    @Test
    void retornaRejeitadaComEncerradaENaoMaior() {
        configurarSessaoEncerradaCom(2L, 8L);

        ResultadoApurado r = service.apurar(1L);

        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.REJEITADA);
    }

    @Test
    void retornaEmpateComEncerradaEContagemIgual() {
        configurarSessaoEncerradaCom(5L, 5L);

        ResultadoApurado r = service.apurar(1L);

        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.EMPATE);
    }

    @Test
    void retornaEmpateZeroVotosComEncerrada() {
        configurarSessaoEncerradaCom(0L, 0L);

        ResultadoApurado r = service.apurar(1L);

        assertThat(r.resultado()).isEqualTo(ResultadoVotacao.EMPATE);
        assertThat(r.totalVotos()).isZero();
    }

    private void configurarSessaoEncerradaCom(long sim, long nao) {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L));
        Sessao encerrada = new Sessao(1L, AGORA.minusMinutes(10), AGORA.minusMinutes(1));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(encerrada));
        given(votoRepository.countByPautaIdAndEscolha(1L, Escolha.SIM)).willReturn(sim);
        given(votoRepository.countByPautaIdAndEscolha(1L, Escolha.NAO)).willReturn(nao);
    }
}
