package br.com.bossolani.judicialpipeline.integration.leiloeiro.dto;

import java.math.BigDecimal;

public class LoteLeilaoDTO {

    private String numeroProcesso;
    private BigDecimal valorAvaliacao;
    private String comarca;
    private String vara;

    private String tipo;
    private String endereco;
    private String numero;
    private String bairro;

    private String urlOrigem;

    public LoteLeilaoDTO(
            String numeroProcesso,
            BigDecimal valorAvaliacao,
            String comarca,
            String vara,
            String tipo,
            String endereco,
            String numero,
            String bairro,
            String urlOrigem
    ) {
        this.numeroProcesso = numeroProcesso;
        this.valorAvaliacao = valorAvaliacao;
        this.comarca = comarca;
        this.vara = vara;
        this.tipo = tipo;
        this.endereco = endereco;
        this.numero = numero;
        this.bairro = bairro;
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

    public String getTipo() {
        return tipo;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getNumero() {
        return numero;
    }

    public String getBairro() {
        return bairro;
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
                ", tipo='" + tipo + '\'' +
                ", endereco='" + endereco + '\'' +
                ", numero='" + numero + '\'' +
                ", bairro='" + bairro + '\'' +
                ", urlOrigem='" + urlOrigem + '\'' +
                '}';
    }
}