package br.com.desafio.votacao.sessao.api;

import br.com.desafio.votacao.sessao.api.dto.AbrirSessaoRequest;
import br.com.desafio.votacao.sessao.api.dto.SessaoResponse;
import br.com.desafio.votacao.sessao.domain.Sessao;
import br.com.desafio.votacao.sessao.service.SessaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessoes")
@Tag(name = "Sessões", description = "Abertura de sessões de votação por pauta")
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @PostMapping
    public ResponseEntity<SessaoResponse> abrir(@PathVariable Long pautaId,
                                                @RequestBody(required = false) AbrirSessaoRequest req) {
        Integer duracao = req == null ? null : req.duracaoMinutos();
        Sessao sessao = sessaoService.abrir(pautaId, duracao);
        URI location = URI.create("/api/v1/pautas/" + pautaId + "/sessoes/" + sessao.getId());
        return ResponseEntity.created(location).body(SessaoResponse.de(sessao));
    }
}
