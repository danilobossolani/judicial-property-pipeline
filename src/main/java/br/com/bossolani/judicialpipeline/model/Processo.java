package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "processos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_processo_numero",
                        columnNames = "numero_processo"
                )
        }
)
public class Processo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "numero_processo",
            nullable = false,
            unique = true,
            length = 20
    )
    private String numeroProcesso;

    private String comarca;

    private String vara;

    private String tribunal;

    private String grau;

    private String orgaoJulgador;

    private String classe;

    private String sistema;

    private String formato;

    private String dataAjuizamento;

    private String ultimaAtualizacao;

    public Processo() {
    }

    public Processo(
            String numeroProcesso,
            String comarca,
            String vara
    ) {

        this.numeroProcesso = numeroProcesso;
        this.comarca = comarca;
        this.vara = vara;
    }

    public Long getId() {

        return id;
    }

    public String getNumeroProcesso() {

        return numeroProcesso;
    }

    public void setNumeroProcesso(
            String numeroProcesso
    ) {

        this.numeroProcesso = numeroProcesso;
    }

    public String getComarca() {

        return comarca;
    }

    public void setComarca(
            String comarca
    ) {

        this.comarca = comarca;
    }

    public String getVara() {

        return vara;
    }

    public void setVara(
            String vara
    ) {

        this.vara = vara;
    }

    public String getTribunal() {

        return tribunal;
    }

    public void setTribunal(
            String tribunal
    ) {

        this.tribunal = tribunal;
    }

    public String getGrau() {

        return grau;
    }

    public void setGrau(
            String grau
    ) {

        this.grau = grau;
    }

    public String getOrgaoJulgador() {

        return orgaoJulgador;
    }

    public void setOrgaoJulgador(
            String orgaoJulgador
    ) {

        this.orgaoJulgador = orgaoJulgador;
    }

    public String getClasse() {

        return classe;
    }

    public void setClasse(
            String classe
    ) {

        this.classe = classe;
    }

    public String getSistema() {

        return sistema;
    }

    public void setSistema(
            String sistema
    ) {

        this.sistema = sistema;
    }

    public String getFormato() {

        return formato;
    }

    public void setFormato(
            String formato
    ) {

        this.formato = formato;
    }

    public String getDataAjuizamento() {

        return dataAjuizamento;
    }

    public void setDataAjuizamento(
            String dataAjuizamento
    ) {

        this.dataAjuizamento = dataAjuizamento;
    }

    public String getUltimaAtualizacao() {

        return ultimaAtualizacao;
    }

    public void setUltimaAtualizacao(
            String ultimaAtualizacao
    ) {

        this.ultimaAtualizacao = ultimaAtualizacao;
    }
}
