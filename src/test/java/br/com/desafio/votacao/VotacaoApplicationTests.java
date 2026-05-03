package br.com.desafio.votacao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class VotacaoApplicationTests {

    @Test
    void contextLoads() {
        // verifica que o contexto Spring sobe sem erros
    }
}
