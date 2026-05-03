package br.com.desafio.votacao.shared.exception;

public class AssociadoNaoPodeVotarException extends NegocioException {

    public AssociadoNaoPodeVotarException(String cpf) {
        super("Associado com CPF %s não está habilitado a votar no momento".formatted(cpf));
    }
}
