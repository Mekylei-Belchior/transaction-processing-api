package com.mekylei.transactionprocessing.infraestrutura.persistencia;

import com.mekylei.transactionprocessing.conta.dominio.Saldo;
import org.junit.jupiter.api.Test;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(SaldoJpaAdapter.class)
class SaldoJpaAdapterTest {

    @Autowired
    private SaldoJpaAdapter adapter;

    @Test
    void findByIdConta_deveRetornarSaldoSalvo() {
        Saldo salvo = adapter.save(novoSaldo());

        Optional<Saldo> encontrado = adapter.findByIdConta(salvo.getIdConta());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getDisponivel()).isEqualByComparingTo("1000.00");
    }

    @Test
    void findByIdContaForUpdate_deveRetornarSaldoComLockPessimista() {
        Saldo salvo = adapter.save(novoSaldo());

        Optional<Saldo> encontrado = adapter.findByIdContaForUpdate(salvo.getIdConta());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getIdConta()).isEqualTo(salvo.getIdConta());
    }

    @Test
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
