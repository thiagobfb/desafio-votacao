package br.com.desafio.votacao.pauta.service;

import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.repository.PautaRepository;
import br.com.desafio.votacao.shared.exception.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PautaService {

    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository pautaRepository;
    private final Clock clock;

    public PautaService(PautaRepository pautaRepository, Clock clock) {
        this.pautaRepository = pautaRepository;
        this.clock = clock;
    }

    @Transactional
    public Pauta criar(String titulo, String descricao) {
        Pauta pauta = new Pauta(titulo, descricao, LocalDateTime.now(clock));
        Pauta salva = pautaRepository.save(pauta);
        log.info("Pauta criada id={} titulo='{}'", salva.getId(), salva.getTitulo());
        return salva;
    }

    @Transactional(readOnly = true)
    public Pauta buscarObrigatorio(Long id) {
        return pautaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta", id));
    }

    @Transactional(readOnly = true)
    public Page<Pauta> listar(Pageable pageable) {
        return pautaRepository.findAll(pageable);
    }
}
