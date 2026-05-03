package br.com.desafio.votacao.pauta.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.repository.PautaRepository;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
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
class PautaServiceTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.of("-03:00");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 5, 1, 12, 0);
    private final Clock clock = Clock.fixed(AGORA.toInstant(BRASILIA), BRASILIA);

    @Mock
    private PautaRepository pautaRepository;

    private PautaService service;

    @BeforeEach
    void setup() {
        service = new PautaService(pautaRepository, clock);
    }

    @Test
    void criaPautaPersistindoComCriadaEmDoClock() {
        given(pautaRepository.save(any(Pauta.class))).willAnswer(inv -> inv.getArgument(0));

        Pauta criada = service.criar("Aprovação do balanço", "descrição opcional");

        assertThat(criada.getTitulo()).isEqualTo("Aprovação do balanço");
        assertThat(criada.getDescricao()).isEqualTo("descrição opcional");
        assertThat(criada.getCriadaEm()).isEqualTo(AGORA);
    }

    @Test
    void criaPautaSemDescricaoQuandoNula() {
        given(pautaRepository.save(any(Pauta.class))).willAnswer(inv -> inv.getArgument(0));

        Pauta criada = service.criar("Título", null);

        assertThat(criada.getDescricao()).isNull();
    }

    @Test
    void buscarObrigatorioRetornaPautaQuandoExiste() {
        Pauta pauta = new Pauta("X", null, AGORA);
        pauta.setId(7L);
        given(pautaRepository.findById(7L)).willReturn(Optional.of(pauta));

        assertThat(service.buscarObrigatorio(7L)).isSameAs(pauta);
    }

    @Test
    void buscarObrigatorioLancaQuandoNaoExiste() {
        given(pautaRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarObrigatorio(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Pauta")
                .hasMessageContaining("99");
    }
}
