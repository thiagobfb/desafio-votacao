package br.com.desafio.votacao.shared.exception;

public class VotoDuplicadoException extends NegocioException {

    public VotoDuplicadoException(Long pautaId, String cpf) {
        super("CPF %s já votou na pauta %d".formatted(cpf, pautaId));
    }
}
