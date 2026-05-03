package br.com.desafio.votacao.voto.api;

import br.com.desafio.votacao.voto.api.dto.RegistrarVotoRequest;
import br.com.desafio.votacao.voto.api.dto.VotoResponse;
import br.com.desafio.votacao.voto.domain.Voto;
import br.com.desafio.votacao.voto.service.VotoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/votos")
@Tag(name = "Votos", description = "Registro de votos de associados em pautas")
public class VotoController {

    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping
    public ResponseEntity<VotoResponse> registrar(@PathVariable Long pautaId,
                                                  @Valid @RequestBody RegistrarVotoRequest req) {
        Voto voto = votoService.registrar(pautaId, req.cpf(), req.voto());
        URI location = URI.create("/api/v1/pautas/" + pautaId + "/votos/" + voto.getId());
        return ResponseEntity.created(location).body(VotoResponse.de(voto));
    }
}
