package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.*;

@Entity
@Table(name = "processos")
public class Processo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroProcesso;
    private String comarca;
    private String vara;

    public Processo() {
    }

    public Processo(String numeroProcesso, String comarca, String vara) {
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

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getComarca() {
        return comarca;
    }

    public void setComarca(String comarca) {
        this.comarca = comarca;
    }

    public String getVara() {
        return vara;
    }

    public void setVara(String vara) {
        this.vara = vara;
    }
}