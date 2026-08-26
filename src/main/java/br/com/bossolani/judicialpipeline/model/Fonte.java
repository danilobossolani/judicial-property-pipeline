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
    @Column(nullable = false)
    private FonteTipo tipo;

    @Column(nullable = false)
    private String origemNome;

    @Column(
            length = 2000,
            unique = true
    )
    private String urlOrigem;

    @Column(nullable = false)
    private LocalDateTime dataCaptura;

    /*
     * Uma fonte pode estar relacionada a um leilão.
     * Exemplo: Sublime Leilões.
     */
    @ManyToOne
    @JoinColumn(name = "leilao_id")
    private Leilao leilao;

    /*
     * Ou diretamente a um processo.
     * Exemplo: DataJud/CNJ.
     */
    @ManyToOne
    @JoinColumn(name = "processo_id")
    private Processo processo;

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

    public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }
}