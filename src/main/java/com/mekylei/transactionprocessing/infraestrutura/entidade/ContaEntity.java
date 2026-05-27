package com.mekylei.transactionprocessing.infraestrutura.entidade;

import com.mekylei.transactionprocessing.auditoria.aplicacao.AuditoriaListener;
import com.mekylei.transactionprocessing.conta.dominio.StatusConta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conta")
@EntityListeners(AuditoriaListener.class)
public class ContaEntity {

    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(name = "numero_conta")
    private String numeroConta;

    @Column(name = "agencia")
    private String agencia;

    @Column(name = "id_cliente")
    private UUID idCliente;

    @Column(name = "tipo")
    @Enumerated(EnumType.STRING)
    private TipoConta tipo;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusConta status;

    @Column(name = "criado_em", updatable = false)
    private Instant criadoEm;

    public ContaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumeroConta() {
        return numeroConta;
    }

    public void setNumeroConta(String numeroConta) {
        this.numeroConta = numeroConta;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public UUID getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(UUID idCliente) {
        this.idCliente = idCliente;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public StatusConta getStatus() {
        return status;
    }

    public void setStatus(StatusConta status) {
        this.status = status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
