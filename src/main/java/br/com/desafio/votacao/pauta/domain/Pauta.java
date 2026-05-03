package br.com.desafio.votacao.pauta.domain;

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
@Table(name = "pauta")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pauta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    public Pauta(String titulo, String descricao, LocalDateTime criadaEm) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.criadaEm = criadaEm;
    }
}
