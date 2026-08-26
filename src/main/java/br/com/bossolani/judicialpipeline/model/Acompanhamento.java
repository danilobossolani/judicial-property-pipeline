package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "acompanhamentos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_acompanhamento_imovel",
                        columnNames = "imovel_id"
                )
        }
)
public class Acompanhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(
            name = "imovel_id",
            nullable = false,
            unique = true
    )
    private Imovel imovel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPipeline statusPipeline;

    @Column(nullable = false)
    private LocalDateTime dataIdentificacao;

    @Column(nullable = false)
    private LocalDateTime ultimaVerificacao;

    @Column(length = 2000)
    private String observacao;

    @Column(nullable = false)
    private boolean ativo;

    public Acompanhamento() {
    }

    public Long getId() {
        return id;
    }

    public Imovel getImovel() {
        return imovel;
    }

    public void setImovel(Imovel imovel) {
        this.imovel = imovel;
    }

    public StatusPipeline getStatusPipeline() {
        return statusPipeline;
    }

    public void setStatusPipeline(StatusPipeline statusPipeline) {
        this.statusPipeline = statusPipeline;
    }

    public LocalDateTime getDataIdentificacao() {
        return dataIdentificacao;
    }

    public void setDataIdentificacao(LocalDateTime dataIdentificacao) {
        this.dataIdentificacao = dataIdentificacao;
    }

    public LocalDateTime getUltimaVerificacao() {
        return ultimaVerificacao;
    }

    public void setUltimaVerificacao(LocalDateTime ultimaVerificacao) {
        this.ultimaVerificacao = ultimaVerificacao;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}