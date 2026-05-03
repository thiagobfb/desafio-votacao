package br.com.desafio.votacao.pauta.service;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.sessao.repository.SessaoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class EstadoPautaResolver {

    private final SessaoRepository sessaoRepository;
    private final Clock clock;

    public EstadoPautaResolver(SessaoRepository sessaoRepository, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.clock = clock;
    }

    public EstadoPauta estadoDe(Long pautaId) {
        return sessaoRepository.findByPautaId(pautaId)
                .map(s -> s.estaAbertaEm(LocalDateTime.now(clock))
                        ? EstadoPauta.SESSAO_ABERTA
                        : EstadoPauta.ENCERRADA)
                .orElse(EstadoPauta.SEM_SESSAO);
    }
}
