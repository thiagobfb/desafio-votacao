package br.com.desafio.votacao.shared.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            RecursoNaoEncontradoException.class,
            CpfInvalidoException.class,
            AssociadoNaoPodeVotarException.class
    })
    public ResponseEntity<ErroResponse> handleNaoEncontrado(NegocioException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(404, ex.getMessage()));
    }

    @ExceptionHandler({
            SessaoJaExisteException.class,
            SessaoEncerradaException.class,
            SessaoNaoAbertaException.class,
            VotoDuplicadoException.class
    })
    public ResponseEntity<ErroResponse> handleConflito(NegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(400, "Falha de validação", erros));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            String campo = ife.getPath().isEmpty()
                    ? "valor"
                    : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            Object[] aceitos = ife.getTargetType().getEnumConstants();
            String msg = "Valor inválido para '%s'. Aceitos: %s".formatted(campo, Arrays.toString(aceitos));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(400, msg));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(400, "Corpo da requisição inválido"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> handleTipoIncompativel(MethodArgumentTypeMismatchException ex) {
        Class<?> tipo = ex.getRequiredType();
        String msg;
        if (tipo != null && tipo.isEnum()) {
            Object[] aceitos = tipo.getEnumConstants();
            msg = "Valor inválido para '%s'. Aceitos: %s".formatted(ex.getName(), Arrays.toString(aceitos));
        } else {
            msg = "Valor inválido para '%s'".formatted(ex.getName());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(400, msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResponse> handleArgumentoIlegal(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(400, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex) {
        log.error("Erro interno não mapeado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponse(500, "Erro interno do servidor"));
    }
}
