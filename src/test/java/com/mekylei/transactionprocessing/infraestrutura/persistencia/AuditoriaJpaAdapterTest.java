package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.auditoria.dominio.AcaoAuditoria;
import com.mekylei.transactionprocessing.auditoria.dominio.AuditoriaEvento;
import com.mekylei.transactionprocessing.infraestrutura.entidade.AuditoriaEventoEntity;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.AuditoriaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(AuditoriaJpaAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuditoriaJpaAdapterTest {

    @Autowired
    private AuditoriaJpaAdapter adapter;

    @Autowired
    private AuditoriaJpaRepository repository;

    @BeforeEach
    void limparAuditoria() {
        repository.deleteAll();
    }

    @Test
    void salvar_devePersistirEventoDeAuditoria() {
        UUID idOperador = UUID.randomUUID();
        UUID idRecurso = UUID.randomUUID();
        UUID idCorrelacao = UUID.randomUUID();
        AuditoriaEvento evento = AuditoriaEvento.simples(
                idOperador, AcaoAuditoria.INSERIR, "Transacao", idRecurso, idCorrelacao, "127.0.0.1");

        adapter.registrar(evento);

        List<AuditoriaEventoEntity> eventos = repository.findAll();
        assertThat(eventos).hasSize(1);
        AuditoriaEventoEntity persistido = eventos.getFirst();
        assertThat(persistido.getId()).isNotNull();
        assertThat(persistido.getIdOperador()).isEqualTo(idOperador);
        assertThat(persistido.getAcao()).isEqualTo(AcaoAuditoria.INSERIR.name());
        assertThat(persistido.getRecurso()).isEqualTo("Transacao");
        assertThat(persistido.getIdRecurso()).isEqualTo(idRecurso);
        assertThat(persistido.getIdCorrelacao()).isEqualTo(idCorrelacao);
        assertThat(persistido.getIpOrigem()).isEqualTo("127.0.0.1");
        assertThat(persistido.getOcorridoEm()).isNotNull();
    }

    @Test
    void salvar_deveSerAppendOnly() {
        UUID idRecurso = UUID.randomUUID();
        adapter.registrar(AuditoriaEvento.simples(
                UUID.randomUUID(), AcaoAuditoria.INSERIR, "Conta", idRecurso, UUID.randomUUID(), "127.0.0.1"));
        adapter.registrar(AuditoriaEvento.simples(
                UUID.randomUUID(), AcaoAuditoria.ATUALIZAR, "Conta", idRecurso, UUID.randomUUID(), "127.0.0.2"));

        List<AuditoriaEventoEntity> eventos = repository.findAll();

        assertThat(eventos).hasSize(2);
        assertThat(eventos)
                .extracting(AuditoriaEventoEntity::getIdRecurso)
                .containsOnly(idRecurso);
        assertThat(eventos)
                .extracting(AuditoriaEventoEntity::getAcao)
                .containsExactlyInAnyOrder(AcaoAuditoria.INSERIR.name(), AcaoAuditoria.ATUALIZAR.name());
    }
}
