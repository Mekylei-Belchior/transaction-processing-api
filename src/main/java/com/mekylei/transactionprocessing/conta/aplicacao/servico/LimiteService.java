package com.mekylei.transactionprocessing.conta.aplicacao.servico;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.conta.aplicacao.porta.repositorio.LimiteRepository;
import com.mekylei.transactionprocessing.conta.dominio.LimiteTransacional;
import com.mekylei.transactionprocessing.compartilhado.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.observabilidade.metrica.TransacaoMetricasNoop;
import com.mekylei.transactionprocessing.observabilidade.metrica.TransacaoMetricasPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class LimiteService {

    private final LimiteRepository limiteRepository;
    private final TransacaoMetricasPort transacaoMetricas;

    @Autowired
    public LimiteService(LimiteRepository limiteRepository, TransacaoMetricasPort transacaoMetricas) {
        this.limiteRepository = limiteRepository;
        this.transacaoMetricas = transacaoMetricas;
    }

    LimiteService(LimiteRepository limiteRepository) {
        this(limiteRepository, new TransacaoMetricasNoop());
    }

    @Transactional(readOnly = true)
    public void validarLimite(UUID idConta, TipoTransacao tipo, BigDecimal valor) {
        LimiteTransacional limite = limiteRepository.findByIdContaAndTipo(idConta, tipo)
                .orElseThrow(() -> new RegraNegocioException(
                        "LIMITE_NAO_CONFIGURADO",
                        "Limite transacional não configurado para o tipo " + tipo + " na conta: " + idConta));
        try {
            limite.validar(ValorMonetario.paraReal(valor));
        } catch (RegraNegocioException e) {
            transacaoMetricas.registrarLimiteExcedido(tipo);
            throw e;
        }
    }

    @Transactional
    public void decrementarUtilizado(UUID idConta, TipoTransacao tipo, BigDecimal valor) {
        Optional<LimiteTransacional> limiteOptional = limiteRepository.findByIdContaAndTipoForUpdate(idConta, tipo);
        limiteOptional.ifPresent(limite -> {
            LimiteTransacional atualizado = limite.decrementar(ValorMonetario.paraReal(valor));
            limiteRepository.save(atualizado);
        });
    }
}
