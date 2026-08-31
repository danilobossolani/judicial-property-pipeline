package br.com.bossolani.judicialpipeline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "execucoes_descoberta")
public class ExecucaoDescoberta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String fonte;

    @Column(nullable = false)
    private LocalDateTime inicio;

    private LocalDateTime termino;

    private Long duracaoMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigemExecucaoDescoberta origem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatusExecucaoDescoberta status;

    @Column(nullable = false)
    private int totalEncontrado;

    @Column(nullable = false)
    private int totalElegivel;

    @Column(nullable = false)
    private int totalImportado;

    @Column(nullable = false)
    private int totalDuplicado;

    @Column(nullable = false)
    private int totalDescartado;

    @Column(nullable = false)
    private int totalFalha;

    @Column(length = 1000)
    private String erroResumo;

    public ExecucaoDescoberta() {
    }

    public Long getId() {
        return id;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getTermino() {
        return termino;
    }

    public void setTermino(LocalDateTime termino) {
        this.termino = termino;
    }

    public Long getDuracaoMs() {
        return duracaoMs;
    }

    public void setDuracaoMs(Long duracaoMs) {
        this.duracaoMs = duracaoMs;
    }

    public OrigemExecucaoDescoberta getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemExecucaoDescoberta origem) {
        this.origem = origem;
    }

    public StatusExecucaoDescoberta getStatus() {
        return status;
    }

    public void setStatus(StatusExecucaoDescoberta status) {
        this.status = status;
    }

    public int getTotalEncontrado() {
        return totalEncontrado;
    }

    public void setTotalEncontrado(int totalEncontrado) {
        this.totalEncontrado = totalEncontrado;
    }

    public int getTotalElegivel() {
        return totalElegivel;
    }

    public void setTotalElegivel(int totalElegivel) {
        this.totalElegivel = totalElegivel;
    }

    public int getTotalImportado() {
        return totalImportado;
    }

    public void setTotalImportado(int totalImportado) {
        this.totalImportado = totalImportado;
    }

    public int getTotalDuplicado() {
        return totalDuplicado;
    }

    public void setTotalDuplicado(int totalDuplicado) {
        this.totalDuplicado = totalDuplicado;
    }

    public int getTotalDescartado() {
        return totalDescartado;
    }

    public void setTotalDescartado(int totalDescartado) {
        this.totalDescartado = totalDescartado;
    }

    public int getTotalFalha() {
        return totalFalha;
    }

    public void setTotalFalha(int totalFalha) {
        this.totalFalha = totalFalha;
    }

    public String getErroResumo() {
        return erroResumo;
    }

    public void setErroResumo(String erroResumo) {
        this.erroResumo = erroResumo;
    }
}
