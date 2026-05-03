package br.com.desafio.votacao.voto.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import br.com.desafio.votacao.cpf.domain.CpfValidator;
import br.com.desafio.votacao.cpf.domain.StatusValidacaoCpf;
import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import br.com.desafio.votacao.shared.exception.AssociadoNaoPodeVotarException;
import br.com.desafio.votacao.shared.exception.CpfInvalidoException;
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
    private static final String CPF = "12345678901";
    private final Clock clock = Clock.fixed(AGORA.toInstant(BRASILIA), BRASILIA);

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoService sessaoService;

    @Mock
    private CpfValidator cpfValidator;

    private VotoService service;

    @BeforeEach
    void setup() {
        service = new VotoService(votoRepository, pautaService, sessaoService, cpfValidator, clock);
    }

    @Test
    void registraVotoComSessaoAberta() {
        given(cpfValidator.validar(CPF)).willReturn(StatusValidacaoCpf.ABLE_TO_VOTE);
        Sessao aberta = new Sessao(1L, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(aberta));
        given(votoRepository.saveAndFlush(any(Voto.class))).willAnswer(inv -> inv.getArgument(0));

        Voto voto = service.registrar(1L, CPF, Escolha.SIM);

        assertThat(voto.getPautaId()).isEqualTo(1L);
        assertThat(voto.getCpf()).isEqualTo(CPF);
        assertThat(voto.getEscolha()).isEqualTo(Escolha.SIM);
        assertThat(voto.getRegistradoEm()).isEqualTo(AGORA);
    }

    @Test
    void rejeitaQuandoPautaNaoTemSessao() {
        given(cpfValidator.validar(CPF)).willReturn(StatusValidacaoCpf.ABLE_TO_VOTE);
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(1L, CPF, Escolha.SIM))
                .isInstanceOf(SessaoNaoAbertaException.class);
    }

    @Test
    void rejeitaSessaoEncerrada() {
        given(cpfValidator.validar(CPF)).willReturn(StatusValidacaoCpf.ABLE_TO_VOTE);
        Sessao expirada = new Sessao(1L, AGORA.minusMinutes(10), AGORA.minusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(expirada));

        assertThatThrownBy(() -> service.registrar(1L, CPF, Escolha.SIM))
                .isInstanceOf(SessaoEncerradaException.class);
    }

    @Test
    void rejeitaSessaoExatamenteNoFechamento() {
        given(cpfValidator.validar(CPF)).willReturn(StatusValidacaoCpf.ABLE_TO_VOTE);
        Sessao limite = new Sessao(1L, AGORA.minusMinutes(5), AGORA);
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(limite));

        assertThatThrownBy(() -> service.registrar(1L, CPF, Escolha.SIM))
                .isInstanceOf(SessaoEncerradaException.class);
    }

    @Test
    void traduzVotoDuplicadoQuandoConstraintEstoura() {
        given(cpfValidator.validar(CPF)).willReturn(StatusValidacaoCpf.ABLE_TO_VOTE);
        Sessao aberta = new Sessao(1L, AGORA.minusMinutes(1), AGORA.plusMinutes(5));
        given(sessaoService.buscarPorPautaId(1L)).willReturn(Optional.of(aberta));
        given(votoRepository.saveAndFlush(any(Voto.class)))
                .willThrow(new DataIntegrityViolationException("uk_voto_pauta_cpf"));

        assertThatThrownBy(() -> service.registrar(1L, CPF, Escolha.SIM))
                .isInstanceOf(VotoDuplicadoException.class)
                .hasMessageContaining(CPF)
                .hasMessageContaining("1");
    }

    @Test
    void rejeitaCpfInvalidoSemConsultarPauta() {
        given(cpfValidator.validar("00000000000")).willReturn(StatusValidacaoCpf.INVALIDO);

        assertThatThrownBy(() -> service.registrar(1L, "00000000000", Escolha.SIM))
                .isInstanceOf(CpfInvalidoException.class)
                .hasMessageContaining("00000000000");
    }

    @Test
    void rejeitaAssociadoNaoHabilitadoSemConsultarPauta() {
        given(cpfValidator.validar(CPF)).willReturn(StatusValidacaoCpf.UNABLE_TO_VOTE);

        assertThatThrownBy(() -> service.registrar(1L, CPF, Escolha.SIM))
                .isInstanceOf(AssociadoNaoPodeVotarException.class)
                .hasMessageContaining(CPF);
    }
}
