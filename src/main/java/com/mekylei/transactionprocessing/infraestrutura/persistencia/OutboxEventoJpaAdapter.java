package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.compartilhado.evento.EventoDominio;
import com.mekylei.transactionprocessing.infraestrutura.entidade.OutboxEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.entidade.StatusOutboxEvento;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.OutboxEventoJpaRepository;
import com.mekylei.transactionprocessing.mensageria.outbox.OutboxEvento;
import com.mekylei.transactionprocessing.mensageria.outbox.OutboxEventoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventoJpaAdapter implements OutboxEventoRepository {

    private final OutboxEventoJpaRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxEventoJpaAdapter(OutboxEventoJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEvento salvar(EventoDominio evento, String topico, String chave) {
        Instant agora = Instant.now();

        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setId(evento.idEvento());
        entity.setTipoEvento(evento.tipoEvento());
        entity.setTipoAgregado(evento.tipoAgregado());
        entity.setIdAgregado(evento.idAgregado());
        entity.setTopico(topico);
        entity.setChave(chave);
        entity.setPayload(objectMapper.valueToTree(evento));
        entity.setIdCorrelacao(evento.idCorrelacao());
        entity.setOcorridoEm(evento.ocorridoEm());
        entity.setCriadoEm(agora);
        entity.setStatus(StatusOutboxEvento.PENDENTE);
        entity.setTentativas(0);
        entity.setProximaTentativaEm(agora);

        return toOutboxEvento(repository.save(entity));
    }

    @Override
    public List<OutboxEvento> buscarParaPublicacao(int tamanhoLote) {
        return repository.buscarParaPublicacao(
                List.of(StatusOutboxEvento.PENDENTE, StatusOutboxEvento.FALHOU),
                Instant.now(),
                PageRequest.of(0, tamanhoLote)
        ).stream().map(this::toOutboxEvento).toList();
    }

    @Override
    public void marcarPublicado(UUID idEvento) {
        OutboxEventoEntity entity = repository.findById(idEvento).orElseThrow(() ->
                new IllegalStateException("OutboxEvento não encontrado para marcar como publicado: " + idEvento));

        entity.setStatus(StatusOutboxEvento.PUBLICADO);
        entity.setPublicadoEm(Instant.now());
        entity.setUltimoErro(null);

        repository.save(entity);
    }

    @Override
    public void marcarFalha(UUID idEvento, Throwable erro, Duration intervaloReprocessamento) {
        OutboxEventoEntity entity = repository.findById(idEvento).orElseThrow(() ->
                new IllegalStateException("OutboxEvento não encontrado para marcar falha: " + idEvento));

        entity.setStatus(StatusOutboxEvento.FALHOU);
        entity.setTentativas(entity.getTentativas() + 1);
        entity.setUltimoErro(truncarMensagemErro(erro));
        entity.setProximaTentativaEm(Instant.now().plus(intervaloReprocessamento));

        repository.save(entity);
    }

    private OutboxEvento toOutboxEvento(OutboxEventoEntity entity) {
        return new OutboxEvento(
                entity.getId(),
                entity.getTipoEvento(),
                entity.getTopico(),
                entity.getChave(),
                entity.getPayload(),
                entity.getTentativas()
        );
    }

    private String truncarMensagemErro(Throwable erro) {
        String mensagem = erro.getMessage();
        if (mensagem == null || mensagem.isBlank()) {
            mensagem = erro.getClass().getSimpleName();
        }
        return mensagem.length() > 2000 ? mensagem.substring(0, 2000) : mensagem;
    }
}
