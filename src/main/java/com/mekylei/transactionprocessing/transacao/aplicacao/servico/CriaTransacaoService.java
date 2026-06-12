package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoUtil;
import com.mekylei.transactionprocessing.observabilidade.metrica.TransacaoMetricasNoop;
import com.mekylei.transactionprocessing.observabilidade.metrica.TransacaoMetricasPort;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.evento.EventoPublicador;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.evento.TransacaoIniciadaEvento;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CriaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(CriaTransacaoService.class);

    private final TransacaoRepository repository;
    private final EventoPublicador eventoPublicador;
    private final TransacaoMetricasPort transacaoMetricas;

    @Autowired
    public CriaTransacaoService(TransacaoRepository repository,
                                EventoPublicador eventoPublicador,
                                TransacaoMetricasPort transacaoMetricas) {
        this.repository = repository;
        this.eventoPublicador = eventoPublicador;
        this.transacaoMetricas = transacaoMetricas;
    }

    CriaTransacaoService(TransacaoRepository repository, EventoPublicador eventoPublicador) {
        this(repository, eventoPublicador, new TransacaoMetricasNoop());
    }

    public Transacao cria(BigDecimal valor,
                          TipoTransacao tipoTransacao,
                          UUID idContaOrigem,
                          String contaDestino,
                          UUID idIdempotencia) {
        UUID idCorrelacao = CorrelacaoUtil.obter();

        logger.info("Criando transação: tipo={}, valor={}, idContaOrigem={}, idCorrelacao={}",
                tipoTransacao, valor, idContaOrigem, idCorrelacao);

        Transacao transacao = Transacao.builder()
                .tipo(tipoTransacao)
                .idCorrelacao(idCorrelacao)
                .idIdempotencia(idIdempotencia)
                .valor(ValorMonetario.paraReal(valor))
                .idContaOrigem(idContaOrigem)
                .contaDestino(contaDestino)
                .build();

        Transacao transacaoSalva = repository.save(transacao);
        transacaoMetricas.registrarTransacaoCriada(transacaoSalva.getTipo(), transacaoSalva.getStatus());
        eventoPublicador.publica(TransacaoIniciadaEvento.de(transacaoSalva));

        logger.info("Transação criada: id={}", transacaoSalva.getId());

        return transacaoSalva;
    }
}
