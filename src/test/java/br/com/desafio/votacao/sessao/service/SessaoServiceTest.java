package br.com.desafio.votacao.sessao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.repository.SessaoRepository;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
import br.com.desafio.votacao.shared.exception.SessaoJaExisteException;
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
class SessaoServiceTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.of("-03:00");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);
    private static final int DEFAULT_DURACAO = 1;
    private static final int MAX_DURACAO = 1440;

    private final Clock clock = Clock.fixed(AGORA.toInstant(BRASILIA), BRASILIA);

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaService pautaService;

    private SessaoService service;

    @BeforeEach
    void setup() {
        service = new SessaoService(sessaoRepository, pautaService, clock, DEFAULT_DURACAO, MAX_DURACAO);
    }

    private Pauta pautaComId(long id) {
        Pauta p = new Pauta("X", null, AGORA);
        p.setId(id);
        return p;
    }

    @Test
    void abreSessaoComDuracaoCustom() {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L));
        given(sessaoRepository.findByPautaId(1L)).willReturn(Optional.empty());
        given(sessaoRepository.save(any(Sessao.class))).willAnswer(inv -> inv.getArgument(0));

        Sessao sessao = service.abrir(1L, 5);

        assertThat(sessao.getPautaId()).isEqualTo(1L);
        assertThat(sessao.getAbertaEm()).isEqualTo(AGORA);
        assertThat(sessao.getFechaEm()).isEqualTo(AGORA.plusMinutes(5));
    }

    @Test
    void abreSessaoUsandoDuracaoDefaultQuandoOmitida() {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L));
        given(sessaoRepository.findByPautaId(1L)).willReturn(Optional.empty());
        given(sessaoRepository.save(any(Sessao.class))).willAnswer(inv -> inv.getArgument(0));

        Sessao sessao = service.abrir(1L, null);

        assertThat(sessao.getFechaEm()).isEqualTo(AGORA.plusMinutes(DEFAULT_DURACAO));
    }

    @Test
    void rejeitaPautaInexistente() {
        given(pautaService.buscarObrigatorio(99L))
                .willThrow(new RecursoNaoEncontradoException("Pauta", 99L));

        assertThatThrownBy(() -> service.abrir(99L, 5))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void rejeitaSegundaSessaoNaMesmaPauta() {
        given(pautaService.buscarObrigatorio(1L)).willReturn(pautaComId(1L));
        given(sessaoRepository.findByPautaId(1L))
                .willReturn(Optional.of(new Sessao(1L, AGORA, AGORA.plusMinutes(5))));

        assertThatThrownBy(() -> service.abrir(1L, 5))
                .isInstanceOf(SessaoJaExisteException.class)
                .hasMessageContaining("1");
    }

    @Test
    void rejeitaDuracaoZero() {
        assertThatThrownBy(() -> service.abrir(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaDuracaoNegativa() {
        assertThatThrownBy(() -> service.abrir(1L, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejeitaDuracaoAcimaDoMaximo() {
        assertThatThrownBy(() -> service.abrir(1L, MAX_DURACAO + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
