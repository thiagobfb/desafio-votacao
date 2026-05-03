package br.com.desafio.votacao.shared.exception;

public class RecursoNaoEncontradoException extends NegocioException {

    public RecursoNaoEncontradoException(String tipo, Object id) {
        super("%s com id %s não encontrado(a)".formatted(tipo, id));
    }
}
