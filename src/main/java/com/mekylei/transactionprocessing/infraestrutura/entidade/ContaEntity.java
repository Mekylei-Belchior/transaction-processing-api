package com.mekylei.transactionprocessing.infraestrutura.entidade;

import com.mekylei.transactionprocessing.conta.dominio.StatusConta;
import com.mekylei.transactionprocessing.conta.dominio.TipoConta;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conta")
public class ContaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero_conta", nullable = false, length = 20)
    private String numeroConta;

    @Column(name = "agencia", nullable = false, length = 10)
    private String agencia;

    @Column(name = "id_cliente", nullable = false)
    private UUID idCliente;

    @Column(name = "tipo", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoConta tipo;

    @Column(name = "status",  nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusConta status;

    @Column(name = "criado_em", nullable = false, updatable = false)
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
