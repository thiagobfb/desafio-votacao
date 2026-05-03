package br.com.desafio.votacao.shared.exception;

public class SessaoJaExisteException extends NegocioException {

    public SessaoJaExisteException(Long pautaId) {
        super("Pauta %d já possui uma sessão de votação".formatted(pautaId));
    }
}
