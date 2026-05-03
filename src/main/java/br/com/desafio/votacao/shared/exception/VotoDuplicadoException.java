package br.com.desafio.votacao.shared.exception;

public class VotoDuplicadoException extends NegocioException {

    public VotoDuplicadoException(Long pautaId, String associadoId) {
        super("Associado %s já votou na pauta %d".formatted(associadoId, pautaId));
    }
}
