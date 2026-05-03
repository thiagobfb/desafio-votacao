package br.com.desafio.votacao.shared.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResponse(
        int status,
        String message,
        List<String> errors,
        LocalDateTime timestamp
) {

    public ErroResponse(int status, String message) {
        this(status, message, List.of(), LocalDateTime.now());
    }

    public ErroResponse(int status, String message, List<String> errors) {
        this(status, message, errors, LocalDateTime.now());
    }
}
