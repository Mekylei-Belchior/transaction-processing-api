package com.mekylei.transactionprocessing.mensageria.evento;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.configuracao.kafka.TopicosProperties;
import com.mekylei.transactionprocessing.transacao.dominio.TipoEventoTransacao;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Optional;

@Component
public class TransacaoEventoRouter {

    private final EnumMap<TipoEventoTransacao, String> topicos;

    public TransacaoEventoRouter(TopicosProperties properties) {
        this.topicos = new EnumMap<>(TipoEventoTransacao.class);

        topicos.put(TipoEventoTransacao.TRANSACAO_INICIADA, properties.transacoesIniciadas());
        topicos.put(TipoEventoTransacao.TRANSACAO_CONCLUIDA, properties.transacoesConcluidas());
        topicos.put(TipoEventoTransacao.TRANSACAO_ESTORNADA, properties.transacoesEstornadas());
        topicos.put(TipoEventoTransacao.TRANSACAO_FALHOU, properties.transacoesFalhas());
    }

    public String resolveTopico(TipoEventoTransacao tipoEvento) {
        return Optional.ofNullable(topicos.get(tipoEvento))
                .orElseThrow(() -> new IllegalArgumentException("Evento não suportado: " + tipoEvento));
    }

    public String resolveChave(EventoDominio evento) {
        return evento.idAgregado().toString();
    }
}
