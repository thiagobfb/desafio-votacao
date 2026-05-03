package br.com.desafio.votacao.shared;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    public MutableClock(Instant initial, ZoneId zone) {
        this.instant = initial;
        this.zone = zone;
    }

    public void definir(Instant momento) {
        this.instant = momento;
    }

    public void avancar(Duration duracao) {
        this.instant = this.instant.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
