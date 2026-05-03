package br.com.desafio.votacao.voto.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "voto")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pauta_id", nullable = false)
    private Long pautaId;

    @Column(nullable = false, length = 64)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Escolha escolha;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    public Voto(Long pautaId, String cpf, Escolha escolha, LocalDateTime registradoEm) {
        this.pautaId = pautaId;
        this.cpf = cpf;
        this.escolha = escolha;
        this.registradoEm = registradoEm;
    }
}
