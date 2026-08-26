package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leiloes")
public class Leilao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1ª Praça
    private LocalDateTime abertura1Praca;

    private LocalDateTime fechamento1Praca;

    @Column(
            precision = 15,
            scale = 2
    )
    private BigDecimal lanceInicial1Praca;

    // 2ª Praça
    private LocalDateTime abertura2Praca;

    private LocalDateTime fechamento2Praca;

    @Column(
            precision = 15,
            scale = 2
    )
    private BigDecimal lanceInicial2Praca;

    // Desconto informado pelo próprio leiloeiro
    private Integer percentualDescontoFonte;

    @Column(
            precision = 15,
            scale = 2
    )
    private BigDecimal lanceMinimo;

    @Column(
            precision = 15,
            scale = 2
    )
    private BigDecimal incremento;

    @Column(
            precision = 5,
            scale = 2
    )
    private BigDecimal comissaoPercentual;

    @Enumerated(EnumType.STRING)
    private StatusLeilao statusLeilao;

    @Enumerated(EnumType.STRING)
    private ResultadoLeilao resultadoLeilao;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "imovel_id",
            nullable = false
    )
    private Imovel imovel;

    public Leilao() {
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getAbertura1Praca() {
        return abertura1Praca;
    }

    public void setAbertura1Praca(LocalDateTime abertura1Praca) {
        this.abertura1Praca = abertura1Praca;
    }

    public LocalDateTime getFechamento1Praca() {
        return fechamento1Praca;
    }

    public void setFechamento1Praca(LocalDateTime fechamento1Praca) {
        this.fechamento1Praca = fechamento1Praca;
    }

    public BigDecimal getLanceInicial1Praca() {
        return lanceInicial1Praca;
    }

    public void setLanceInicial1Praca(BigDecimal lanceInicial1Praca) {
        this.lanceInicial1Praca = lanceInicial1Praca;
    }

    public LocalDateTime getAbertura2Praca() {
        return abertura2Praca;
    }

    public void setAbertura2Praca(LocalDateTime abertura2Praca) {
        this.abertura2Praca = abertura2Praca;
    }

    public LocalDateTime getFechamento2Praca() {
        return fechamento2Praca;
    }

    public void setFechamento2Praca(LocalDateTime fechamento2Praca) {
        this.fechamento2Praca = fechamento2Praca;
    }

    public BigDecimal getLanceInicial2Praca() {
        return lanceInicial2Praca;
    }

    public void setLanceInicial2Praca(BigDecimal lanceInicial2Praca) {
        this.lanceInicial2Praca = lanceInicial2Praca;
    }

    public Integer getPercentualDescontoFonte() {
        return percentualDescontoFonte;
    }

    public void setPercentualDescontoFonte(Integer percentualDescontoFonte) {
        this.percentualDescontoFonte = percentualDescontoFonte;
    }

    public BigDecimal getLanceMinimo() {
        return lanceMinimo;
    }

    public void setLanceMinimo(BigDecimal lanceMinimo) {
        this.lanceMinimo = lanceMinimo;
    }

    public BigDecimal getIncremento() {
        return incremento;
    }

    public void setIncremento(BigDecimal incremento) {
        this.incremento = incremento;
    }

    public BigDecimal getComissaoPercentual() {
        return comissaoPercentual;
    }

    public void setComissaoPercentual(BigDecimal comissaoPercentual) {
        this.comissaoPercentual = comissaoPercentual;
    }

    public StatusLeilao getStatusLeilao() {
        return statusLeilao;
    }

    public void setStatusLeilao(StatusLeilao statusLeilao) {
        this.statusLeilao = statusLeilao;
    }

    public ResultadoLeilao getResultadoLeilao() {
        return resultadoLeilao;
    }

    public void setResultadoLeilao(ResultadoLeilao resultadoLeilao) {
        this.resultadoLeilao = resultadoLeilao;
    }

    public Imovel getImovel() {
        return imovel;
    }

    public void setImovel(Imovel imovel) {
        this.imovel = imovel;
    }
}