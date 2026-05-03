package br.com.desafio.votacao.voto.service;

import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import br.com.desafio.votacao.shared.exception.SessaoEncerradaException;
import br.com.desafio.votacao.shared.exception.SessaoNaoAbertaException;
import br.com.desafio.votacao.shared.exception.VotoDuplicadoException;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.domain.Voto;
import br.com.desafio.votacao.voto.repository.VotoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    private final VotoRepository votoRepository;
    private final PautaService pautaService;
    private final SessaoService sessaoService;
    private final Clock clock;

    public VotoService(VotoRepository votoRepository,
                       PautaService pautaService,
                       SessaoService sessaoService,
                       Clock clock) {
        this.votoRepository = votoRepository;
        this.pautaService = pautaService;
        this.sessaoService = sessaoService;
        this.clock = clock;
    }

    @Transactional
    public Voto registrar(Long pautaId, String associadoId, Escolha escolha) {
        pautaService.buscarObrigatorio(pautaId);

        Sessao sessao = sessaoService.buscarPorPautaId(pautaId)
                .orElseThrow(() -> {
                    log.warn("Voto rejeitado: pauta sem sessão pautaId={} associadoId={}", pautaId, associadoId);
                    return new SessaoNaoAbertaException(pautaId);
                });

        LocalDateTime agora = LocalDateTime.now(clock);
        if (!sessao.estaAbertaEm(agora)) {
            log.warn("Voto rejeitado: sessão encerrada pautaId={} associadoId={}", pautaId, associadoId);
            throw new SessaoEncerradaException(pautaId);
        }

        Voto voto = new Voto(pautaId, associadoId, escolha, agora);
        try {
            Voto salvo = votoRepository.saveAndFlush(voto);
            log.info("Voto registrado id={} pautaId={} associadoId={} escolha={}",
                    salvo.getId(), pautaId, associadoId, escolha);
            return salvo;
        } catch (DataIntegrityViolationException e) {
            log.warn("Voto rejeitado: duplicado pautaId={} associadoId={}", pautaId, associadoId);
            throw new VotoDuplicadoException(pautaId, associadoId);
        }
    }
}
