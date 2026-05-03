package br.com.desafio.votacao.resultado.api;

import br.com.desafio.votacao.resultado.api.dto.ResultadoResponse;
import br.com.desafio.votacao.resultado.service.ResultadoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/resultado")
@Tag(name = "Resultado", description = "Apuração de votos por pauta")
public class ResultadoController {

    private final ResultadoService resultadoService;

    public ResultadoController(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    @GetMapping
    public ResultadoResponse apurar(@PathVariable Long pautaId) {
        return ResultadoResponse.de(resultadoService.apurar(pautaId));
    }
}
