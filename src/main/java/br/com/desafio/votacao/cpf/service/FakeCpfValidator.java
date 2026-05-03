package br.com.desafio.votacao.cpf.service;

import br.com.desafio.votacao.cpf.domain.CpfValidator;
import br.com.desafio.votacao.cpf.domain.StatusValidacaoCpf;
import java.security.SecureRandom;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validador fake do desafio (Tarefa Bônus 1).
 *
 * <p>A validação é dividida em duas etapas:
 * <ol>
 *   <li><b>Formato (determinístico):</b> aplica o algoritmo dos dígitos verificadores do CPF
 *       (DV1 + DV2). CPF que não passa retorna {@link StatusValidacaoCpf#INVALIDO}.</li>
 *   <li><b>Habilitação (aleatória):</b> CPF estruturalmente válido recebe aleatoriamente
 *       {@link StatusValidacaoCpf#ABLE_TO_VOTE} ou {@link StatusValidacaoCpf#UNABLE_TO_VOTE},
 *       como pede o enunciado: <i>"Essa operação retorna resultados aleatórios, portanto um
 *       mesmo CPF pode funcionar em um teste e não funcionar no outro."</i></li>
 * </ol>
 *
 * <p>Aceita CPF com ou sem formatação (`111.444.777-35` e `11144477735` produzem o mesmo resultado);
 * caracteres não numéricos são removidos antes da checagem.
 */
@Component
public class FakeCpfValidator implements CpfValidator {

    private static final Logger log = LoggerFactory.getLogger(FakeCpfValidator.class);

    private final Random random;

    public FakeCpfValidator() {
        this(new SecureRandom());
    }

    FakeCpfValidator(Random random) {
        this.random = random;
    }

    @Override
    public StatusValidacaoCpf validar(String cpf) {
        if (!formatoValido(cpf)) {
            log.debug("CPF rejeitado por formato cpf={}", cpf);
            return StatusValidacaoCpf.INVALIDO;
        }
        StatusValidacaoCpf habilitacao = random.nextBoolean()
                ? StatusValidacaoCpf.ABLE_TO_VOTE
                : StatusValidacaoCpf.UNABLE_TO_VOTE;
        log.debug("CPF estruturalmente válido cpf={} habilitacao={}", cpf, habilitacao);
        return habilitacao;
    }

    /**
     * Aplica o algoritmo dos dígitos verificadores (DV1 + DV2) descrito na Receita Federal.
     *
     * <pre>
     * Soma1 = d1*10 + d2*9 + d3*8 + d4*7 + d5*6 + d6*5 + d7*4 + d8*3 + d9*2
     * Resto1 = Soma1 % 11
     * DV1 = (Resto1 < 2) ? 0 : 11 - Resto1
     *
     * Soma2 = d1*11 + d2*10 + d3*9 + d4*8 + d5*7 + d6*6 + d7*5 + d8*4 + d9*3 + DV1*2
     * Resto2 = Soma2 % 11
     * DV2 = (Resto2 < 2) ? 0 : 11 - Resto2
     * </pre>
     *
     * <p>O CPF é considerado válido quando d10 == DV1 e d11 == DV2.
     */
    private boolean formatoValido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String numeros = cpf.replaceAll("\\D", "");
        if (numeros.length() != 11) {
            return false;
        }

        int[] d = new int[11];
        for (int i = 0; i < 11; i++) {
            d[i] = numeros.charAt(i) - '0';
        }

        int soma1 = d[0] * 10 + d[1] * 9 + d[2] * 8 + d[3] * 7 + d[4] * 6
                + d[5] * 5 + d[6] * 4 + d[7] * 3 + d[8] * 2;
        int resto1 = soma1 % 11;
        int dv1 = (resto1 < 2) ? 0 : 11 - resto1;
        if (d[9] != dv1) {
            return false;
        }

        int soma2 = d[0] * 11 + d[1] * 10 + d[2] * 9 + d[3] * 8 + d[4] * 7
                + d[5] * 6 + d[6] * 5 + d[7] * 4 + d[8] * 3 + dv1 * 2;
        int resto2 = soma2 % 11;
        int dv2 = (resto2 < 2) ? 0 : 11 - resto2;
        return d[10] == dv2;
    }
}
