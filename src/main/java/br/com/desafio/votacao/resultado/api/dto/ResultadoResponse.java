package br.com.desafio.votacao.resultado.api.dto;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.resultado.domain.ResultadoApurado;
import br.com.desafio.votacao.resultado.domain.ResultadoVotacao;

public record ResultadoResponse(
        Long pautaId,
        EstadoPauta estado,
        long totalSim,
        long totalNao,
        long totalVotos,
        ResultadoVotacao resultado
) {

    public static ResultadoResponse de(ResultadoApurado apurado) {
        return new ResultadoResponse(
                apurado.pautaId(),
                apurado.estado(),
                apurado.totalSim(),
                apurado.totalNao(),
                apurado.totalVotos(),
                apurado.resultado()
        );
    }
}
