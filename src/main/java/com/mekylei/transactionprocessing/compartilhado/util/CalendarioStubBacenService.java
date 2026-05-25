package com.mekylei.transactionprocessing.compartilhado.util;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

@Service
public class CalendarioStubBacenService {

    // Feriados nacionais fixos (mês/dia)
    private static final Set<MonthDay> FERIADOS_FIXOS = Set.of(
            MonthDay.of(1, 1),    // Confraternização Universal
            MonthDay.of(4, 21),   // Tiradentes
            MonthDay.of(5, 1),    // Dia do Trabalho
            MonthDay.of(9, 7),    // Independência do Brasil
            MonthDay.of(10, 12),  // Nossa Senhora Aparecida
            MonthDay.of(11, 2),   // Finados
            MonthDay.of(11, 15),  // Proclamação da República
            MonthDay.of(11, 20),  // Consciência Negra (Lei 14.759/2023)
            MonthDay.of(12, 25)   // Natal
    );

    /**
     * Retorna true se a data é um dia útil bancário brasileiro.
     * Considera: fim de semana, feriados nacionais fixos e feriados móveis
     * baseados em Páscoa (Carnaval, Sexta-feira Santa, Corpus Christi).
     */
    public boolean isDiaUtil(LocalDate data) {
        DayOfWeek diaDaSemana = data.getDayOfWeek();
        if (diaDaSemana == DayOfWeek.SATURDAY || diaDaSemana == DayOfWeek.SUNDAY) {
            return false;
        }
        if (FERIADOS_FIXOS.contains(MonthDay.from(data))) {
            return false;
        }
        return !calcularFeriadosMoveis(data.getYear()).contains(data);
    }

    /**
     * Calcula os feriados nacionais móveis para o ano informado,
     * derivados da data da Páscoa.
     */
    private Set<LocalDate> calcularFeriadosMoveis(int ano) {
        LocalDate pascoa = calcularPascoa(ano);
        return Set.of(
                pascoa.minusDays(48),  // Segunda-feira de Carnaval
                pascoa.minusDays(47),  // Terça-feira de Carnaval
                pascoa.minusDays(2),   // Sexta-feira Santa (Paixão de Cristo)
                pascoa.plusDays(60)       // Corpus Christi
        );
    }

    /**
     * Algoritmo de Meeus/Jones/Butcher — calcula a Páscoa para qualquer ano gregoriano.
     */
    private LocalDate calcularPascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
