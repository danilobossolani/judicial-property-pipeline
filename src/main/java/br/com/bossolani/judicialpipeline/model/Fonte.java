package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fontes")
public class Fonte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private FonteTipo tipo;

    private String origemNome;

    @Column(length = 2000)
    private String urlOrigem;

    private LocalDateTime dataCaptura;

    @ManyToOne(optional = false)
    @JoinColumn(name = "leilao_id", nullable = false)
    private Leilao leilao;

    public Fonte() {
    }

    public Long getId() {
        return id;
    }

    public FonteTipo getTipo() {
        return tipo;
    }

    public void setTipo(FonteTipo tipo) {
        this.tipo = tipo;
    }

    public String getOrigemNome() {
        return origemNome;
    }

    public void setOrigemNome(String origemNome) {
        this.origemNome = origemNome;
    }

    public String getUrlOrigem() {
        return urlOrigem;
    }

    public void setUrlOrigem(String urlOrigem) {
        this.urlOrigem = urlOrigem;
    }

    public LocalDateTime getDataCaptura() {
        return dataCaptura;
    }

    public void setDataCaptura(LocalDateTime dataCaptura) {
        this.dataCaptura = dataCaptura;
    }

    public Leilao getLeilao() {
        return leilao;
    }

    public void setLeilao(Leilao leilao) {
        this.leilao = leilao;
    }
}