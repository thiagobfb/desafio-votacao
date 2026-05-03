package br.com.desafio.votacao.shared.exception;

public class CpfInvalidoException extends NegocioException {

    public CpfInvalidoException(String cpf) {
        super("CPF %s inválido ou não encontrado".formatted(cpf));
    }
}
