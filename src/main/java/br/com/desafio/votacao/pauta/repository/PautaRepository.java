package br.com.desafio.votacao.pauta.repository;

import br.com.desafio.votacao.pauta.domain.Pauta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PautaRepository extends JpaRepository<Pauta, Long> {
}
