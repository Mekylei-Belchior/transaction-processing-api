package com.mekylei.transactionprocessing.infraestrutura.entidade;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;


import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
public class AuditoriaEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id_operador", updatable = false)
    private UUID idOperador;

    @Column(name = "acao", updatable = false)
    private String acao;

    @Column(name = "recurso", updatable = false)
    private String recurso;

    @Column(name = "id_recurso", updatable = false)
    private UUID idRecurso;

    @Column(name = "id_correlacao", updatable = false)
    private UUID idCorrelacao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_anteriores", columnDefinition = "jsonb", updatable = false)
    private JsonNode dadosAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados_novos", columnDefinition = "jsonb", updatable = false)
    private JsonNode dadosNovos;

    @Column(name = "ip_origem", updatable = false)
    private String ipOrigem;

    @Column(name = "ocorrido_em", updatable = false)
    private Instant ocorridoEm;

    public AuditoriaEventoEntity() {

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIdOperador() {
        return idOperador;
    }

    public void setIdOperador(UUID idOperador) {
        this.idOperador = idOperador;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }

    public UUID getIdRecurso() {
        return idRecurso;
    }

    public void setIdRecurso(UUID idRecurso) {
        this.idRecurso = idRecurso;
    }

    public UUID getIdCorrelacao() {
        return idCorrelacao;
    }

    public void setIdCorrelacao(UUID idCorrelacao) {
        this.idCorrelacao = idCorrelacao;
    }

    public JsonNode getDadosAnteriores() {
        return dadosAnteriores;
    }

    public void setDadosAnteriores(JsonNode dadosAnteriores) {
        this.dadosAnteriores = dadosAnteriores;
    }

    public JsonNode getDadosNovos() {
        return dadosNovos;
    }

    public void setDadosNovos(JsonNode dadosNovos) {
        this.dadosNovos = dadosNovos;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(Instant ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }
}