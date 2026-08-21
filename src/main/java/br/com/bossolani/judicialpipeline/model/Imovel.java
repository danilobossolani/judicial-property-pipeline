package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

@Entity
@Table(name = "imoveis")
public class Imovel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;

    private String cidade;

    private String bairro;

    private Double valorAvaliacao;

    private String status;

    public Imovel() {
    }

    public Imovel(String tipo, String cidade, String bairro,
                  Double valorAvaliacao, String status) {
        this.tipo = tipo;
        this.cidade = cidade;
        this.bairro = bairro;
        this.valorAvaliacao = valorAvaliacao;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public Double getValorAvaliacao() {
        return valorAvaliacao;
    }

    public void setValorAvaliacao(Double valorAvaliacao) {
        this.valorAvaliacao = valorAvaliacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @ManyToOne
    @JoinColumn(name = "processo_id")
    private Processo processo;

    public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }
}   