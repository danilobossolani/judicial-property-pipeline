package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resultados_lote_descoberta")
public class ResultadoLoteDescoberta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "execucao_id", nullable = false)
    private ExecucaoDescoberta execucao;

    @Column(length = 500)
    private String titulo;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2000)
    private String urlOriginal;

    @Column(length = 2000)
    private String urlNormalizada;

    @Column(length = 30)
    private String numeroProcesso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DecisaoLoteDescoberta decisao;

    @Column(nullable = false, length = 1000)
    private String motivo;

    @ManyToOne
    @JoinColumn(name = "imovel_id")
    private Imovel imovel;

    public ResultadoLoteDescoberta() {
    }

    public Long getId() {
        return id;
    }

    public ExecucaoDescoberta getExecucao() {
        return execucao;
    }

    public void setExecucao(ExecucaoDescoberta execucao) {
        this.execucao = execucao;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUrlOriginal() {
        return urlOriginal;
    }

    public void setUrlOriginal(String urlOriginal) {
        this.urlOriginal = urlOriginal;
    }

    public String getUrlNormalizada() {
        return urlNormalizada;
    }

    public void setUrlNormalizada(String urlNormalizada) {
        this.urlNormalizada = urlNormalizada;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public DecisaoLoteDescoberta getDecisao() {
        return decisao;
    }

    public void setDecisao(DecisaoLoteDescoberta decisao) {
        this.decisao = decisao;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Imovel getImovel() {
        return imovel;
    }

    public void setImovel(Imovel imovel) {
        this.imovel = imovel;
    }
}
