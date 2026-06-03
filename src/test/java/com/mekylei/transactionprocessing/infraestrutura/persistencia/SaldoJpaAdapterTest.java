package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.conta.dominio.Saldo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para {@link SaldoJpaAdapter}.
 *
 * <p>Objetivo:</p>
 * <ul>
 *     <li>Validar o comportamento esperado de {@link SaldoJpaAdapter} nos cenários exercitados pela suíte.</li>
 *     <li>Preservar regras de negócio, contratos, integrações ou invariantes aplicáveis à classe testada.</li>
 *     <li>Garantir regressão funcional para alterações futuras relacionadas a {@code SaldoJpaAdapter}.</li>
 * </ul>
 *
 * <p>Cenários cobertos:</p>
 * <ul>
 *     <li>FindByIdConta deve retornar saldo salvo.</li>
 *     <li>FindByIdContaForUpdate deve retornar saldo com lock pessimista.</li>
 *     <li>Save deve atualizar saldo existente.</li>
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
@Import(SaldoJpaAdapter.class)
@DisplayName("Saldo Jpa Adapter")
class SaldoJpaAdapterTest {

    @Autowired
    private SaldoJpaAdapter adapter;

    @Test
    @DisplayName("findByIdConta deve retornar saldo salvo")
    void findByIdConta_deveRetornarSaldoSalvo() {
        Saldo salvo = adapter.save(novoSaldo());

        Optional<Saldo> encontrado = adapter.findByIdConta(salvo.getIdConta());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getDisponivel()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("findByIdContaForUpdate deve retornar saldo com lock pessimista")
    void findByIdContaForUpdate_deveRetornarSaldoComLockPessimista() {
        Saldo salvo = adapter.save(novoSaldo());

        Optional<Saldo> encontrado = adapter.findByIdContaForUpdate(salvo.getIdConta());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getIdConta()).isEqualTo(salvo.getIdConta());
    }

    @Test
    @DisplayName("save deve atualizar saldo existente")
    void save_deveAtualizarSaldoExistente() {
        Saldo salvo = adapter.save(novoSaldo());
        Saldo alterado = Saldo.builder()
                .id(salvo.getId())
                .idConta(salvo.getIdConta())
                .disponivel(new BigDecimal("750.25"))
                .bloqueado(new BigDecimal("25.00"))
                .versao(salvo.getVersao())
                .atualizadoEm(Instant.now())
                .build();

        adapter.save(alterado);

        Optional<Saldo> encontrado = adapter.findByIdConta(salvo.getIdConta());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getDisponivel()).isEqualByComparingTo("750.25");
        assertThat(encontrado.get().getBloqueado()).isEqualByComparingTo("25.00");
    }

    private Saldo novoSaldo() {
        return Saldo.builder()
                .idConta(UUID.randomUUID())
                .disponivel(new BigDecimal("1000.00"))
                .bloqueado(BigDecimal.ZERO)
                .build();
    }
}
