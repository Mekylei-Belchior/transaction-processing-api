package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.compartilhado.dominio.ValorMonetario;
import com.mekylei.transactionprocessing.infraestrutura.repositorio.TransacaoJpaRepository;
import com.mekylei.transactionprocessing.transacao.dominio.StatusTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.TipoTransacao;
import com.mekylei.transactionprocessing.transacao.dominio.Transacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link TransacaoJpaAdapter}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link TransacaoJpaAdapter} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code TransacaoJpaAdapter}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>Save deve retornar transação com ID gerado.</li>
 *     <li>FindById deve retornar transação salva.</li>
 *     <li>FindById deve retornar empty para ID inexistente.</li>
 *     <li>FindByIdIdempotencia deve retornar transação com chave correta.</li>
 *     <li>FindByIdIdempotencia deve retornar empty para chave inexistente.</li>
 *     <li>Update deve atualizar status da transação.</li>
 * </ul>
 *
 * <p>Cenários não cobertos:</p>
 * <ul>
 *     <li>Testes de carga, resiliência distribuída e validações de infraestrutura externas ao escopo da classe.</li>
 * </ul>
 *
 * @author Mekylei Belchior
 * @since 1.0
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(TransacaoJpaAdapter.class)
@DisplayName("Transacao Jpa Adapter")
class TransacaoJpaAdapterTest {

    @Autowired
    private TransacaoJpaAdapter adapter;

    @Autowired
    private TransacaoJpaRepository repository;

    @Test
    @DisplayName("save deve retornar transação com ID gerado")
    void save_deveRetornarTransacaoComIdGerado() {
        Transacao salva = adapter.save(novaTransacao());

        assertThat(salva.getId()).isNotNull();
        assertThat(repository.findById(salva.getId())).isPresent();
    }

    @Test
    @DisplayName("findById deve retornar transação salva")
    void findById_deveRetornarTransacaoSalva() {
        Transacao salva = adapter.save(novaTransacao());

        Optional<Transacao> encontrada = adapter.findById(salva.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getStatus()).isEqualTo(StatusTransacao.PENDENTE);
    }

    @Test
    @DisplayName("findById deve retornar empty para ID inexistente")
    void findById_deveRetornarEmptyParaIdInexistente() {
        Optional<Transacao> encontrada = adapter.findById(UUID.randomUUID());

        assertThat(encontrada).isEmpty();
    }

    @Test
    @DisplayName("findByIdIdempotencia deve retornar transação com chave correta")
    void findByIdIdempotencia_deveRetornarTransacaoComChaveCorreta() {
        UUID idIdempotencia = UUID.randomUUID();
        Transacao salva = adapter.save(novaTransacaoComIdempotencia(idIdempotencia));

        Optional<Transacao> encontrada = adapter.findByIdIdempotencia(idIdempotencia);

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getId()).isEqualTo(salva.getId());
        assertThat(encontrada.get().getIdIdempotencia()).isEqualTo(idIdempotencia);
    }

    @Test
    @DisplayName("findByIdIdempotencia deve retornar empty para chave inexistente")
    void findByIdIdempotencia_deveRetornarEmptyParaChaveInexistente() {
        Optional<Transacao> encontrada = adapter.findByIdIdempotencia(UUID.randomUUID());

        assertThat(encontrada).isEmpty();
    }

    @Test
    @DisplayName("update deve atualizar status da transação")
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
