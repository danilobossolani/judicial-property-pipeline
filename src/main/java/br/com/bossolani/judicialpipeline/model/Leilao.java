package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leiloes")
public class Leilao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double valorMinimo2Praca;

    private LocalDateTime data1Praca;

    private LocalDateTime data2Praca;

    @Enumerated(EnumType.STRING)
    private StatusLeilao statusLeilao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "imovel_id", nullable = false)
    private Imovel imovel;

    public Leilao() {
    }

    public Long getId() {
        return id;
    }

    public Double getValorMinimo2Praca() {
        return valorMinimo2Praca;
    }

    public void setValorMinimo2Praca(Double valorMinimo2Praca) {
        this.valorMinimo2Praca = valorMinimo2Praca;
    }

    public LocalDateTime getData1Praca() {
        return data1Praca;
    }

    public void setData1Praca(LocalDateTime data1Praca) {
        this.data1Praca = data1Praca;
    }

    public LocalDateTime getData2Praca() {
        return data2Praca;
    }

    public void setData2Praca(LocalDateTime data2Praca) {
        this.data2Praca = data2Praca;
    }

    public StatusLeilao getStatusLeilao() {
        return statusLeilao;
    }

    public void setStatusLeilao(StatusLeilao statusLeilao) {
        this.statusLeilao = statusLeilao;
    }

    public Imovel getImovel() {
        return imovel;
    }

    public void setImovel(Imovel imovel) {
        this.imovel = imovel;
    }
}