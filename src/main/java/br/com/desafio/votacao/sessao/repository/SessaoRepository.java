package br.com.desafio.votacao.sessao.repository;

import br.com.desafio.votacao.sessao.domain.Sessao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    Optional<Sessao> findByPautaId(Long pautaId);
}
