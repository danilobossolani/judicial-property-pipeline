package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Entity
@Table(name = "imoveis")
public class Imovel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;

    @Column(length = 1000)
    private String endereco;

    private String numero;

    private String complemento;

    private String bairro;

    private String cidade;

    private String cep;

    @Column(
            name = "valor_avaliacao",
            precision = 15,
            scale = 2
    )
    private BigDecimal valorAvaliacao;

    @NotNull(message = "O processo é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(
            name = "processo_id",
            nullable = false
    )
    private Processo processo;

    public Imovel() {
    }

    public Imovel(
            String tipo,
            String endereco,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String cep,
            BigDecimal valorAvaliacao,
            Processo processo
    ) {
        this.tipo = tipo;
        this.endereco = endereco;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.cep = cep;
        this.valorAvaliacao = valorAvaliacao;
        this.processo = processo;
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

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public BigDecimal getValorAvaliacao() {
        return valorAvaliacao;
    }

    public void setValorAvaliacao(BigDecimal valorAvaliacao) {
        this.valorAvaliacao = valorAvaliacao;
    }

    public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }
}