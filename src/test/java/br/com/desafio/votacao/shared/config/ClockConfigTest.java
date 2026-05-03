package br.com.desafio.votacao.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ClockConfigTest {

    @Autowired
    private Clock clock;

    @Test
    void clockBeanIsAvailableAndUsesBrasiliaOffset() {
        assertThat(clock).isNotNull();
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.of("-03:00"));
    }
}
