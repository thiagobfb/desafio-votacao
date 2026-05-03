package br.com.desafio.votacao.voto.api.dto;

import br.com.desafio.votacao.voto.domain.Voto;
import java.time.LocalDateTime;

public record VotoResponse(Long votoId, LocalDateTime registradoEm) {

    public static VotoResponse de(Voto voto) {
        return new VotoResponse(voto.getId(), voto.getRegistradoEm());
    }
}
