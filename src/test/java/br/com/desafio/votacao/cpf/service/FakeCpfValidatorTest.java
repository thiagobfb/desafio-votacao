package br.com.desafio.votacao.cpf.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.desafio.votacao.cpf.domain.StatusValidacaoCpf;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FakeCpfValidatorTest {

    /**
     * CPFs com DV1 e DV2 corretos pela aritmética da Receita.
     * Calculados manualmente (e conferidos no validador).
     */
    private static final String CPF_VALIDO = "11144477735";
    private static final String CPF_VALIDO_FORMATADO = "111.444.777-35";

    @Test
    void cpfValidoNuncaRetornaInvalidoEAceitaAmbasAsHabilitacoes() {
        FakeCpfValidator v = new FakeCpfValidator(new Random(42L));

        Set<StatusValidacaoCpf> vistos = EnumSet.noneOf(StatusValidacaoCpf.class);
        for (int i = 0; i < 50; i++) {
            StatusValidacaoCpf s = v.validar(CPF_VALIDO);
            assertThat(s).isNotEqualTo(StatusValidacaoCpf.INVALIDO);
            vistos.add(s);
        }

        // Com Random(42) e 50 chamadas, vemos os dois estados habilitados.
        assertThat(vistos)
                .containsExactlyInAnyOrder(StatusValidacaoCpf.ABLE_TO_VOTE, StatusValidacaoCpf.UNABLE_TO_VOTE);
    }

    @Test
    void cpfValidoComFormatacaoEhAceito() {
        FakeCpfValidator v = new FakeCpfValidator(new Random(42L));

        StatusValidacaoCpf s = v.validar(CPF_VALIDO_FORMATADO);

        assertThat(s).isNotEqualTo(StatusValidacaoCpf.INVALIDO);
    }

    /**
     * CPFs que falham o algoritmo de DV. Cada caso explora uma falha distinta:
     * DV1 errado, DV2 errado, comprimento incorreto, não-numérico, formato inválido.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "11144477734",       // DV2 errado (correto = 35)
            "11144477745",       // DV1 errado (correto = 3)
            "12345678901",       // ambos os DVs errados
            "00000000001",       // DV1 ok, DV2 errado
            "1234567890",        // 10 dígitos
            "111444777355",      // 12 dígitos
            "abcdefghijk",       // não-numérico
            "111.444.777-99",    // formatado mas com DVs errados
            "          "         // só espaços
    })
    void cpfInvalidoRetornaInvalido(String cpf) {
        FakeCpfValidator v = new FakeCpfValidator(new Random(0L));

        assertThat(v.validar(cpf)).isEqualTo(StatusValidacaoCpf.INVALIDO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void cpfNullOuVazioRetornaInvalido(String cpf) {
        FakeCpfValidator v = new FakeCpfValidator(new Random(0L));

        assertThat(v.validar(cpf)).isEqualTo(StatusValidacaoCpf.INVALIDO);
    }

    /**
     * Outro CPF válido (39053344705 — exemplo recorrente em literatura sobre CPF).
     * Verifica que o algoritmo não está "decorando" um único exemplar.
     */
    @Test
    void outroCpfValidoTambemEhAceito() {
        FakeCpfValidator v = new FakeCpfValidator(new Random(7L));

        StatusValidacaoCpf s = v.validar("39053344705");

        assertThat(s).isNotEqualTo(StatusValidacaoCpf.INVALIDO);
    }

    /**
     * Garante que o sorteio entre ABLE/UNABLE realmente cobre os dois estados em runs diferentes
     * (não está fixo num só por causa da implementação).
     */
    @Test
    void distribuicaoEntreAbleEUnableAtingeAmbosOsLados() {
        FakeCpfValidator v = new FakeCpfValidator(new Random(123L));

        long ables = 0;
        long unables = 0;
        for (int i = 0; i < 200; i++) {
            StatusValidacaoCpf s = v.validar(CPF_VALIDO);
            if (s == StatusValidacaoCpf.ABLE_TO_VOTE) {
                ables++;
            } else {
                unables++;
            }
        }
        assertThat(ables).isGreaterThan(50);
        assertThat(unables).isGreaterThan(50);
    }
}
