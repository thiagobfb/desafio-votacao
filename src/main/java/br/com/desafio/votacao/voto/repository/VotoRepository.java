package br.com.desafio.votacao.voto.repository;

import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.domain.Voto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    long countByPautaIdAndEscolha(Long pautaId, Escolha escolha);

    long countByPautaId(Long pautaId);

    boolean existsByPautaIdAndCpf(Long pautaId, String cpf);
}
