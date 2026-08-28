package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "historicos_acompanhamento")
public class HistoricoAcompanhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "acompanhamento_id",
            nullable = false
    )
    private Acompanhamento acompanhamento;

    @Column(nullable = false)
    private LocalDateTime dataEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPipeline statusPipeline;

    @Enumerated(EnumType.STRING)
    private StatusLeilao statusLeilao;

    @Enumerated(EnumType.STRING)
    private ResultadoLeilao resultadoLeilao;

    @Column(nullable = false)
    private String origem;

    @Column(length = 2000)
    private String descricao;

    public HistoricoAcompanhamento() {
    }

    public Long getId() {
        return id;
    }

    public Acompanhamento getAcompanhamento() {
        return acompanhamento;
    }

    public void setAcompanhamento(
            Acompanhamento acompanhamento
    ) {
        this.acompanhamento = acompanhamento;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(
            LocalDateTime dataEvento
    ) {
        this.dataEvento = dataEvento;
    }

    public StatusPipeline getStatusPipeline() {
        return statusPipeline;
    }

    public void setStatusPipeline(
            StatusPipeline statusPipeline
    ) {
        this.statusPipeline = statusPipeline;
    }

    public StatusLeilao getStatusLeilao() {
        return statusLeilao;
    }

    public void setStatusLeilao(
            StatusLeilao statusLeilao
    ) {
        this.statusLeilao = statusLeilao;
    }

    public ResultadoLeilao getResultadoLeilao() {
        return resultadoLeilao;
    }

    public void setResultadoLeilao(
            ResultadoLeilao resultadoLeilao
    ) {
        this.resultadoLeilao = resultadoLeilao;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}