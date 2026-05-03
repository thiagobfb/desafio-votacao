package br.com.desafio.votacao.pauta.api.dto;

import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.pauta.domain.Pauta;
import java.time.LocalDateTime;

public record PautaResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime criadaEm,
        EstadoPauta estado
) {

    public static PautaResponse de(Pauta pauta, EstadoPauta estado) {
        return new PautaResponse(
                pauta.getId(),
                pauta.getTitulo(),
                pauta.getDescricao(),
                pauta.getCriadaEm(),
                estado
        );
    }
}
