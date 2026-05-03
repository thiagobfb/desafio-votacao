package br.com.desafio.votacao.voto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import br.com.desafio.votacao.shared.exception.SessaoEncerradaException;
import br.com.desafio.votacao.shared.exception.SessaoNaoAbertaException;
import br.com.desafio.votacao.shared.exception.VotoDuplicadoException;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.domain.Voto;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.of("-03:00");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);
    private final Clock clock = Clock.fixed(AGORA.toInstant(BRASILIA), BRASILIA);

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoService sessaoService;

    private VotoService service;

    @BeforeEach
    void setup() {
        service = new VotoService(votoRepository, pautaService, sessaoService, clock);
    }

    @Test
    void registraVotoComSessaoAberta() {
        Sessao aberta = new Sessao(1L, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(aberta));
        given(votoRepository.saveAndFlush(any(Voto.class))).willAnswer(inv -> inv.getArgument(0));

        Voto voto = service.registrar(1L, "A1", Escolha.SIM);

        assertThat(voto.getPautaId()).isEqualTo(1L);
        assertThat(voto.getAssociadoId()).isEqualTo("A1");
        assertThat(voto.getEscolha()).isEqualTo(Escolha.SIM);
        assertThat(voto.getRegistradoEm()).isEqualTo(AGORA);
    }

    @Test
    void rejeitaQuandoPautaNaoTemSessao() {
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(1L, "A1", Escolha.SIM))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }

    @Test
    void rejeitaSessaoEncerrada() {
        Sessao expirada = new Sessao(1L, AGORA.minusMinutes(10), AGORA.minusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(expirada));

        assertThatThrownBy(() -> service.registrar(1L, "A1", Escolha.SIM))
                .isInstanceOf(SessaoEncerradaException.class);
    }

    @Test
    void rejeitaSessaoExatamenteNoFechamento() {
        Sessao limite = new Sessao(1L, AGORA.minusMinutes(5), AGORA);
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(limite));

        assertThatThrownBy(() -> service.registrar(1L, "A1", Escolha.SIM))
                .isInstanceOf(SessaoEncerradaException.class);
    }

    @Test
    void traduzVotoDuplicadoQuandoConstraintEstoura() {
        Sessao aberta = new Sessao(1L, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(aberta));
        given(votoRepository.saveAndFlush(any(Voto.class)))
                .willThrow(new DataIntegrityViolationException("uk_voto_pauta_associado"));

        assertThatThrownBy(() -> service.registrar(1L, "A1", Escolha.SIM))
                .isInstanceOf(VotoDuplicadoException.class)
                .hasMessageContaining("A1")
                .hasMessageContaining("1");
    }
}
