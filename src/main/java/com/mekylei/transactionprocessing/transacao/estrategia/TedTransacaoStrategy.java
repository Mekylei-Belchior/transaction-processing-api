package com.mekylei.transactionprocessing.transacao.estrategia;


import com.mekylei.transactionprocessing.compartilhado.exception.RegraNegocioException;
import com.mekylei.transactionprocessing.compartilhado.util.CalendarioStubBacenService;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class TedTransacaoStrategy implements TransacaoStrategy {

    private static final LocalTime BANCO_TED_INICIO = LocalTime.of(6, 0);
    private static final LocalTime BANCO_TED_FIM = LocalTime.of(17, 0);
    private static final ZoneId BRASIL_TIMEZONE = ZoneId.of("America/Sao_Paulo");

    private static final Logger logger = LoggerFactory.getLogger(TedTransacaoStrategy.class);

    private final CalendarioStubBacenService calendarioService;

    public TedTransacaoStrategy(CalendarioStubBacenService calendarioService) {
        this.calendarioService = calendarioService;
    }

    @Override
    public boolean suporta(TipoTransacao tipoTransacao) {
        return TipoTransacao.TED == tipoTransacao;
    }

    @Override
    public Transacao processa(Transacao transacao) {
        logger.info("Processando TED: id={}, valor={}, idCorrelacao={}",
                transacao.getId(), transacao.getValor(), transacao.getIdCorrelacao());

        validarDiaUtil();
        validaHorarioPermitido();

        enviarParaSistemaTransferenciaReserva(transacao);

        return transacao.comStatus(StatusTransacao.COMPLETADA);
    }

    private void enviarParaSistemaTransferenciaReserva(Transacao transacao) {
        logger.debug("Simulando envio para o STR a transação: {}", transacao.getId());
    }

    private void validaHorarioPermitido() {
        LocalTime agora = LocalTime.now(BRASIL_TIMEZONE);
        boolean foraDeHorario = agora.isBefore(BANCO_TED_INICIO) || agora.isAfter(BANCO_TED_FIM);

        if (foraDeHorario) {
            throw new RegraNegocioException(
                    "TED_FORA_DO_HORARIO",
                    "TED disponível apenas entre " + BANCO_TED_INICIO + " e " + BANCO_TED_FIM + " (horário de Brasília)"
            );
        }
    }

    private void validarDiaUtil() {
        LocalDate hoje = LocalDate.now(BRASIL_TIMEZONE);
        if (!calendarioService.isDiaUtil(hoje)) {
            throw new RegraNegocioException(
                    "TED_DIA_NAO_UTIL",
                    "TED não disponível em fins de semana ou feriados bancários. Data: " + hoje
            );
        }
    }
}
