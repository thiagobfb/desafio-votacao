package br.com.desafio.votacao.voto.repository;

import br.com.desafio.votacao.voto.domain.ContagemPorEscolha;
import br.com.desafio.votacao.voto.domain.Escolha;
import br.com.desafio.votacao.voto.domain.Voto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    long countByPautaIdAndEscolha(Long pautaId, Escolha escolha);

    long countByPautaId(Long pautaId);

    boolean existsByPautaIdAndCpf(Long pautaId, String cpf);

    /**
     * Agrega votos por escolha para uma pauta em uma única query (`GROUP BY`).
     * Substitui as duas chamadas de `countByPautaIdAndEscolha` na apuração — corta o número
     * de round-trips ao banco pela metade, mantendo o mesmo plano de execução indexado em
     * `idx_voto_pauta`.
     */
    @Query("""
            SELECT new br.com.desafio.votacao.voto.domain.ContagemPorEscolha(v.escolha, COUNT(v))
            FROM Voto v
            WHERE v.pautaId = :pautaId
            GROUP BY v.escolha
            """)
    List<ContagemPorEscolha> agregarVotosPorEscolha(@Param("pautaId") Long pautaId);
}
