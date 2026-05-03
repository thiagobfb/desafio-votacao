package br.com.desafio.votacao.sessao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sessao")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sessao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(name = "aberta_em", nullable = false)
    private LocalDateTime abertaEm;

    @Column(name = "fecha_em", nullable = false)
    private LocalDateTime fechaEm;

    public Sessao(Long pautaId, LocalDateTime abertaEm, LocalDateTime fechaEm) {
        this.pautaId = pautaId;
        this.abertaEm = abertaEm;
        this.fechaEm = fechaEm;
    }

    public boolean estaAbertaEm(LocalDateTime momento) {
        return !momento.isBefore(abertaEm) && momento.isBefore(fechaEm);
    }
}
