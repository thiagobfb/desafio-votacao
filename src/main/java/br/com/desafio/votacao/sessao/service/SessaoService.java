package br.com.desafio.votacao.sessao.service;

import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.repository.SessaoRepository;
import br.com.desafio.votacao.shared.exception.SessaoJaExisteException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoService.class);

    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;
    private final Clock clock;
    private final int duracaoDefaultMinutos;
    private final int duracaoMaximaMinutos;

    public SessaoService(SessaoRepository sessaoRepository,
                         PautaService pautaService,
                         Clock clock,
                         @Value("${votacao.sessao.duracao-default-minutos}") int duracaoDefaultMinutos,
                         @Value("${votacao.sessao.duracao-maxima-minutos}") int duracaoMaximaMinutos) {
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
        this.clock = clock;
        this.duracaoDefaultMinutos = duracaoDefaultMinutos;
        this.duracaoMaximaMinutos = duracaoMaximaMinutos;
    }

    @Transactional
    public Sessao abrir(Long pautaId, Integer duracaoMinutos) {
        int duracao = duracaoMinutos == null ? duracaoDefaultMinutos : duracaoMinutos;
        if (duracao <= 0 || duracao > duracaoMaximaMinutos) {
            throw new IllegalArgumentException(
                    "Duração deve estar entre 1 e %d minutos".formatted(duracaoMaximaMinutos));
        }

        pautaService.buscarObrigatorio(pautaId);

        if (sessaoRepository.findByPautaId(pautaId).isPresent()) {
            log.warn("Tentativa de abrir segunda sessão para pauta id={}", pautaId);
            throw new SessaoJaExisteException(pautaId);
        }

        LocalDateTime abertaEm = LocalDateTime.now(clock);
        LocalDateTime fechaEm = abertaEm.plusMinutes(duracao);
        Sessao sessao = sessaoRepository.save(new Sessao(pautaId, abertaEm, fechaEm));
        log.info("Sessão aberta id={} pautaId={} duracaoMinutos={} fechaEm={}",
                sessao.getId(), pautaId, duracao, fechaEm);
        return sessao;
    }

    @Transactional(readOnly = true)
    public Optional<Sessao> buscarPorPautaId(Long pautaId) {
        return sessaoRepository.findByPautaId(pautaId);
    }
}
