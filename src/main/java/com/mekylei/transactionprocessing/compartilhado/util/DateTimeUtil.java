package com.mekylei.transactionprocessing.compartilhado.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    private static final ZoneId BRASIL_TIMEZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_BRASIL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS z");

    private DateTimeUtil() {}

    /**
     * Retorna o ZonedDateTime atual no fuso brasileiro
     */
    public static ZonedDateTime agora() {
        return ZonedDateTime.now(BRASIL_TIMEZONE);
    }

    /**
     * Converte um Instant para ZonedDateTime no fuso brasileiro
     */
    public static ZonedDateTime paraFusoBrasil(Instant instant) {
        return instant.atZone(BRASIL_TIMEZONE);
    }

    /**
     * Retorna o Instant atual formatado como "string" no padrão brasileiro BRT
     */
    public static String agoraFormatoBr() {
        return agora().format(FORMATO_BRASIL);
    }

    /**
     * Retorna o Instant atual com offset de São Paulo (-03:00 ou −02:00 no horário de verão)
     */
    public static String agoraComOffset() {
        return agora().getOffset().toString();
    }

    /**
     * Verifica se está em horário de verão
     */
    public static boolean estaEmHorarioVerao() {
        return BRASIL_TIMEZONE.getRules().isDaylightSavings(Instant.now());
    }
}
