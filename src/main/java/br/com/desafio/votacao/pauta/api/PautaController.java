package br.com.desafio.votacao.pauta.api;

import br.com.desafio.votacao.pauta.api.dto.CriarPautaRequest;
import br.com.desafio.votacao.pauta.api.dto.PautaResponse;
import br.com.desafio.votacao.pauta.domain.EstadoPauta;
import br.com.desafio.votacao.pauta.domain.Pauta;
import br.com.desafio.votacao.pauta.service.EstadoPautaResolver;
import br.com.desafio.votacao.pauta.service.PautaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas")
@Tag(name = "Pautas", description = "Cadastro e consulta de pautas para votação")
public class PautaController {

    private final PautaService pautaService;
    private final EstadoPautaResolver estadoResolver;

    public PautaController(PautaService pautaService, EstadoPautaResolver estadoResolver) {
        this.pautaService = pautaService;
        this.estadoResolver = estadoResolver;
    }

    @PostMapping
    public ResponseEntity<PautaResponse> criar(@Valid @RequestBody CriarPautaRequest req) {
        Pauta pauta = pautaService.criar(req.titulo(), req.descricao());
        URI location = URI.create("/api/v1/pautas/" + pauta.getId());
        return ResponseEntity.created(location)
                .body(PautaResponse.de(pauta, EstadoPauta.SEM_SESSAO));
    }

    @GetMapping("/{id}")
    public PautaResponse buscar(@PathVariable Long id) {
        Pauta pauta = pautaService.buscarObrigatorio(id);
        return PautaResponse.de(pauta, estadoResolver.estadoDe(pauta.getId()));
    }

    @GetMapping
    public Page<PautaResponse> listar(@PageableDefault(size = 20) Pageable pageable) {
        return pautaService.listar(pageable)
                .map(p -> PautaResponse.de(p, estadoResolver.estadoDe(p.getId())));
    }
}
