package br.com.desafio.votacao.shared.exception;

public class SessaoNaoAbertaException extends NegocioException {

    public SessaoNaoAbertaException(Long pautaId) {
        super("Pauta %d não possui sessão de votação aberta".formatted(pautaId));
    }
}
