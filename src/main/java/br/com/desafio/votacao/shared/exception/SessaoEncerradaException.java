package br.com.desafio.votacao.shared.exception;

public class SessaoEncerradaException extends NegocioException {

    public SessaoEncerradaException(Long pautaId) {
        super("Sessão de votação da pauta %d está encerrada".formatted(pautaId));
    }
}
