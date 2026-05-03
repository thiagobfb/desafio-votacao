package br.com.desafio.votacao.shared.config;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    public static final ZoneOffset HORARIO_BRASILIA = ZoneOffset.of("-03:00");

    @Bean
    public Clock clock() {
        return Clock.system(HORARIO_BRASILIA);
    }
}
