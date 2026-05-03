package br.com.desafio.votacao.sessao.api.dto;

import br.com.desafio.votacao.sessao.domain.Sessao;
import java.time.LocalDateTime;

public record SessaoResponse(
        Long sessaoId,
        Long pautaId,
        LocalDateTime abertaEm,
        LocalDateTime fechaEm
) {

    public static SessaoResponse de(Sessao sessao) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPautaId(),
                sessao.getAbertaEm(),
                sessao.getFechaEm()
        );
    }
}
