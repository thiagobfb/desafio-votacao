package br.com.desafio.votacao.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.repository.PautaRepository;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.repository.SessaoRepository;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.domain.Voto;
import br.com.desafio.votacao.voto.repository.VotoRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PersistenciaIntegracaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 4, 30, 15, 0);

    @Autowired
    private PautaRepository pautas;

    @Autowired
    private SessaoRepository sessoes;

    @Autowired
    private VotoRepository votos;

    @Test
    void persistePautaERecuperaPorId() {
        Pauta pauta = pautas.saveAndFlush(new Pauta("Aprovação do balanço", "descrição opcional", AGORA));

        assertThat(pauta.getId()).isNotNull();
        assertThat(pautas.findById(pauta.getId())).isPresent().get().satisfies(encontrada -> {
            assertThat(encontrada.getTitulo()).isEqualTo("Aprovação do balanço");
            assertThat(encontrada.getDescricao()).isEqualTo("descrição opcional");
            assertThat(encontrada.getCriadaEm()).isEqualTo(AGORA);
        });
    }

    @Test
    void persisteESssaoEEncontraPorPautaId() {
        Pauta pauta = pautas.saveAndFlush(new Pauta("Pauta X", null, AGORA));
        Sessao sessao = sessoes.saveAndFlush(new Sessao(pauta.getId(), AGORA, AGORA.plusMinutes(5)));

        assertThat(sessoes.findByPautaId(pauta.getId())).isPresent().get().satisfies(encontrada -> {
            assertThat(encontrada.getId()).isEqualTo(sessao.getId());
            assertThat(encontrada.getFechaEm()).isEqualTo(AGORA.plusMinutes(5));
        });
    }

    @Test
    void uniqueConstraintImpedeDuasSessoesNaMesmaPauta() {
        Pauta pauta = pautas.saveAndFlush(new Pauta("Pauta X", null, AGORA));
        sessoes.saveAndFlush(new Sessao(pauta.getId(), AGORA, AGORA.plusMinutes(5)));

        Sessao segunda = new Sessao(pauta.getId(), AGORA, AGORA.plusMinutes(10));
        assertThatThrownBy(() -> sessoes.saveAndFlush(segunda))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueConstraintImpedeVotoDuplicadoDoMesmoCpfNaPauta() {
        Pauta pauta = pautas.saveAndFlush(new Pauta("Pauta X", null, AGORA));
        votos.saveAndFlush(new Voto(pauta.getId(), "11111111111", Escolha.SIM, AGORA));

        Voto duplicado = new Voto(pauta.getId(), "11111111111", Escolha.NAO, AGORA);
        assertThatThrownBy(() -> votos.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void contagemPorEscolhaFunciona() {
        Pauta pauta = pautas.saveAndFlush(new Pauta("Pauta X", null, AGORA));
        votos.saveAndFlush(new Voto(pauta.getId(), "11111111111", Escolha.SIM, AGORA));
        votos.saveAndFlush(new Voto(pauta.getId(), "22222222222", Escolha.SIM, AGORA));
        votos.saveAndFlush(new Voto(pauta.getId(), "33333333333", Escolha.NAO, AGORA));

        assertThat(votos.countByPautaIdAndEscolha(pauta.getId(), Escolha.SIM)).isEqualTo(2);
        assertThat(votos.countByPautaIdAndEscolha(pauta.getId(), Escolha.NAO)).isEqualTo(1);
        assertThat(votos.countByPautaId(pauta.getId())).isEqualTo(3);
    }

    @Test
    void existsByPautaIdAndCpfDetectaVotoExistente() {
        Pauta pauta = pautas.saveAndFlush(new Pauta("Pauta X", null, AGORA));
        votos.saveAndFlush(new Voto(pauta.getId(), "11111111111", Escolha.SIM, AGORA));

        assertThat(votos.existsByPautaIdAndCpf(pauta.getId(), "11111111111")).isTrue();
        assertThat(votos.existsByPautaIdAndCpf(pauta.getId(), "22222222222")).isFalse();
    }

    @Test
    void sessaoSabeQuandoEstaAberta() {
        Sessao sessao = new Sessao(1L, AGORA, AGORA.plusMinutes(5));

        assertThat(sessao.estaAbertaEm(AGORA)).isTrue();
        assertThat(sessao.estaAbertaEm(AGORA.plusMinutes(2))).isTrue();
        assertThat(sessao.estaAbertaEm(AGORA.plusMinutes(5))).isFalse();
        assertThat(sessao.estaAbertaEm(AGORA.minusSeconds(1))).isFalse();
    }
}
