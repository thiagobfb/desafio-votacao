package br.com.desafio.votacao.resultado.service;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.service.PautaService;
import br.com.desafio.votacao.resultado.domain.ResultadoApurado;
import br.com.desafio.votacao.resultado.domain.ResultadoVotacao;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.repository.VotoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoService {

    private static final Logger log = LoggerFactory.getLogger(ResultadoService.class);

    private final PautaService pautaService;
    private final SessaoService sessaoService;
    private final VotoRepository votoRepository;
    private final Clock clock;

    public ResultadoService(PautaService pautaService,
                            SessaoService sessaoService,
                            VotoRepository votoRepository,
                            Clock clock) {
        this.pautaService = pautaService;
        this.sessaoService = sessaoService;
        this.votoRepository = votoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResultadoApurado apurar(Long pautaId) {
        Pauta pauta = pautaService.buscarObrigatorio(pautaId);
        Optional<Sessao> sessaoOpt = sessaoService.buscarPorPautaId(pautaId);

        long totalSim = votoRepository.countByPautaIdAndEscolha(pautaId, Escolha.SIM);
        long totalNao = votoRepository.countByPautaIdAndEscolha(pautaId, Escolha.NAO);
        long total = totalSim + totalNao;

        if (sessaoOpt.isEmpty()) {
            return new ResultadoApurado(pauta.getId(), EstadoPauta.SEM_SESSAO,
                    totalSim, totalNao, total, ResultadoVotacao.SEM_SESSAO);
        }

        Sessao sessao = sessaoOpt.get();
        LocalDateTime agora = LocalDateTime.now(clock);

        if (sessao.estaAbertaEm(agora)) {
            return new ResultadoApurado(pauta.getId(), EstadoPauta.SESSAO_ABERTA,
                    totalSim, totalNao, total, ResultadoVotacao.EM_ANDAMENTO);
        }

        ResultadoVotacao resultado;
        if (totalSim > totalNao) {
            resultado = ResultadoVotacao.APROVADA;
        } else if (totalNao > totalSim) {
            resultado = ResultadoVotacao.REJEITADA;
        } else {
            resultado = ResultadoVotacao.EMPATE;
        }

        log.info("Sessão expirada detectada na apuração pautaId={} sessaoId={} resultado={} totalVotos={}",
                pautaId, sessao.getId(), resultado, total);
        return new ResultadoApurado(pauta.getId(), EstadoPauta.ENCERRADA,
                totalSim, totalNao, total, resultado);
    }
}
