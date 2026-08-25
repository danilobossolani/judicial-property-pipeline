package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

import java.math.BigDecimal;

public class LoteLeilaoDTO {

    private String numeroProcesso;
    private BigDecimal valorAvaliacao;
    private String comarca;
    private String vara;
    private String endereco;
    private String urlOrigem;

    public LoteLeilaoDTO(
            String numeroProcesso,
            BigDecimal valorAvaliacao,
            String comarca,
            String vara,
            String endereco,
            String urlOrigem
    ) {
        this.numeroProcesso = numeroProcesso;
        this.valorAvaliacao = valorAvaliacao;
        this.comarca = comarca;
        this.vara = vara;
        this.endereco = endereco;
        this.urlOrigem = urlOrigem;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public BigDecimal getValorAvaliacao() {
        return valorAvaliacao;
    }

    public String getComarca() {
        return comarca;
    }

    public String getVara() {
        return vara;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getUrlOrigem() {
        return urlOrigem;
    }

    @Override
    public String toString() {
        return "LoteLeilaoDTO{" +
                "numeroProcesso='" + numeroProcesso + '\'' +
                ", valorAvaliacao=" + valorAvaliacao +
                ", comarca='" + comarca + '\'' +
                ", vara='" + vara + '\'' +
                ", endereco='" + endereco + '\'' +
                ", urlOrigem='" + urlOrigem + '\'' +
                '}';
    }
}