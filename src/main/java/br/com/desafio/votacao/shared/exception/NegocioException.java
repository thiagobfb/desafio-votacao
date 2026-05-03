package br.com.desafio.votacao.shared.exception;

public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String message) {
        super(message);
    }
}
