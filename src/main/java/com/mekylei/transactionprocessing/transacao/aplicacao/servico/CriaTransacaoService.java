package com.mekylei.transactionprocessing.transacao.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.util.CorrelacaoUtil;
import com.mekylei.transactionprocessing.transacao.aplicacao.porta.repositorio.TransacaoRepository;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import com.mekylei.transactionprocessing.transacao.dominio.vo.ValorMonetario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CriaTransacaoService {

    private static final Logger logger = LoggerFactory.getLogger(CriaTransacaoService.class);

    private final TransacaoRepository repository;

    public CriaTransacaoService(TransacaoRepository repository) {
        this.repository = repository;
    }

    public Transacao cria(BigDecimal valor, TipoTransacao tipoTransacao, UUID idContaOrigem, String contaDestino, UUID idIdempotencia) {
        UUID idCorrelacao = CorrelacaoUtil.gerarIdCorrelacao();

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

        logger.info("Transação criada: id={}", transacaoSalva.getId());

        return transacaoSalva;
    }
}
