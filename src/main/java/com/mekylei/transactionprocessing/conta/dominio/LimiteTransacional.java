package com.mekylei.transactionprocessing.conta.dominio;

import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.vo.ValorMonetario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LimiteTransacional {

    private final UUID id;
    private final UUID idConta;
    private final TipoTransacao tipo;
    private final BigDecimal limiteDiario;
    private final BigDecimal limiteTransacao;
    private final BigDecimal utilizadoHoje;
    private final LocalDate dataReferencia;

    private LimiteTransacional(Builder builder) {
        this.id = builder.id;
        this.idConta = builder.idConta;
        this.tipo = builder.tipo;
        this.limiteDiario = builder.limiteDiario;
        this.limiteTransacao = builder.limiteTransacao;
        this.utilizadoHoje = builder.utilizadoHoje;
        this.dataReferencia = builder.dataReferencia;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Valida se a operação respeita limite por transação e limite diário.
     * Não modifica estado — somente lança exceção se inválido.
     */
    public void validar(ValorMonetario valorMonetario) {
        BigDecimal valor = valorMonetario.valor();

        if (valor.compareTo(this.limiteTransacao) > 0) {
            throw new RegraNegocioException(
                    "LIMITE_POR_TRANSACAO_EXCEDIDO",
                    String.format("Valor R$ %s excede o limite por transação de R$ %s para %s",
                            valor, this.limiteTransacao, this.tipo));
        }

        if (this.utilizadoHoje.add(valor).compareTo(this.limiteDiario) > 0) {
            throw new RegraNegocioException(
                    "LIMITE_DIARIO_EXCEDIDO",
                    String.format("Operação de R$ %s excede o limite diário de R$ %s para %s. Utilizado hoje: R$ %s",
                            valor, this.limiteDiario, this.tipo, this.utilizadoHoje));
        }
    }

    /**
     * Valida e retorna nova instância com utilizadoHoje incrementado.
     * Deve ser chamado após validar() ou como operação atômica de validação + decremento.
     */
    public LimiteTransacional decrementar(ValorMonetario valor) {
        validar(valor);
        return new Builder()
                .id(this.id)
                .idConta(this.idConta)
                .tipo(this.tipo)
                .limiteDiario(this.limiteDiario)
                .limiteTransacao(this.limiteTransacao)
                .utilizadoHoje(this.utilizadoHoje.add(valor.valor()))
                .dataReferencia(this.dataReferencia)
                .build();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdConta() {
        return idConta;
    }

    public TipoTransacao getTipo() {
        return tipo;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    public BigDecimal getLimiteTransacao() {
        return limiteTransacao;
    }

    public BigDecimal getUtilizadoHoje() {
        return utilizadoHoje;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public static final class Builder {
        private UUID id;
        private UUID idConta;
        private TipoTransacao tipo;
        private BigDecimal limiteDiario;
        private BigDecimal limiteTransacao;
        private BigDecimal utilizadoHoje = BigDecimal.ZERO;
        private LocalDate dataReferencia;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder idConta(UUID idConta) {
            this.idConta = idConta;
            return this;
        }

        public Builder tipo(TipoTransacao tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder limiteDiario(BigDecimal valor) {
            this.limiteDiario = valor;
            return this;
        }

        public Builder limiteTransacao(BigDecimal valor) {
            this.limiteTransacao = valor;
            return this;
        }

        public Builder utilizadoHoje(BigDecimal valor) {
            this.utilizadoHoje = valor;
            return this;
        }

        public Builder dataReferencia(LocalDate data) {
            this.dataReferencia = data;
            return this;
        }

        public LimiteTransacional build() {
            if (idConta == null) throw new IllegalStateException("'idConta' é obrigatório");
            if (tipo == null) throw new IllegalStateException("'tipo' é obrigatório");
            if (limiteDiario == null) throw new IllegalStateException("'limiteDiario' é obrigatório");
            if (limiteTransacao == null) throw new IllegalStateException("'limiteTransacao' é obrigatório");

            if (id == null) id = UUID.randomUUID();
            if (dataReferencia == null) dataReferencia = LocalDate.now();

            return new LimiteTransacional(this);
        }
    }
}