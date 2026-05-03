package br.com.desafio.votacao.resultado.domain;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;

public record ResultadoApurado(
        Long pautaId,
        EstadoPauta estado,
        long totalSim,
        long totalNao,
        long totalVotos,
        ResultadoVotacao resultado
) {
}
