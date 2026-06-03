package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.TransacaoJpaRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TransacaoJpaAdapter.class)
class TransacaoJpaAdapterTest {

    @Autowired
    private TransacaoJpaAdapter adapter;

    @Autowired
    private TransacaoJpaRepository repository;

    @Test
    void save_deveRetornarTransacaoComIdGerado() {
        Transacao salva = adapter.save(novaTransacao());

        assertThat(salva.getId()).isNotNull();
        assertThat(repository.findById(salva.getId())).isPresent();
    }

    @Test
    void findById_deveRetornarTransacaoSalva() {
        Transacao salva = adapter.save(novaTransacao());

        Optional<Transacao> encontrada = adapter.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    @Test
    void findById_deveRetornarEmptyParaIdInexistente() {
        Optional<Transacao> encontrada = adapter.findById(UUID.randomUUID());

        assertThat(encontrada).isEmpty();
    }

    @Test
    void findByIdIdempotencia_deveRetornarTransacaoComChaveCorreta() {
        UUID idIdempotencia = UUID.randomUUID();
        Transacao salva = adapter.save(novaTransacaoComIdempotencia(idIdempotencia));

        Optional<Transacao> encontrada = adapter.findByIdIdempotencia(idIdempotencia);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getIdIdempotencia()).isEqualTo(idIdempotencia);
    }

    @Test
    void findByIdIdempotencia_deveRetornarEmptyParaChaveInexistente() {
        Optional<Transacao> encontrada = adapter.findByIdIdempotencia(UUID.randomUUID());

        assertThat(encontrada).isEmpty();
    }

    @Test
    void update_deveAtualizarStatusDaTransacao() {
        Transacao salva = adapter.save(novaTransacao());

        adapter.update(salva.comStatus(StatusTransacao.COMPLETADA));

        Optional<Transacao> atualizada = adapter.findById(salva.getId());
        assertThat(atualizada).isPresent();
        assertThat(atualizada.get().getStatus()).isEqualTo(StatusTransacao.COMPLETADA);
    }

    private Transacao novaTransacao() {
        return novaTransacaoComIdempotencia(UUID.randomUUID());
    }

    private Transacao novaTransacaoComIdempotencia(UUID idIdempotencia) {
        return Transacao.builder()
                .idCorrelacao(UUID.randomUUID())
                .idIdempotencia(idIdempotencia)
                .valor(ValorMonetario.paraReal(new BigDecimal("125.90")))
                .tipo(TipoTransacao.PIX)
                .status(StatusTransacao.PENDENTE)
                .idContaOrigem(UUID.randomUUID())
                .contaDestino("0001-12345-9")
                .build();
    }
}
