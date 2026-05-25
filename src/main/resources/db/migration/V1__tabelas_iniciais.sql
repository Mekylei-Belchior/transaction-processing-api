create table conta(
    id uuid not null,
    agencia varchar(10) not null,
    criado_em timestamptz(6) not null,
    id_cliente uuid not null,
    numero_conta varchar(20) not null,
    status varchar(20) not null,
    tipo varchar(20) not null,
    constraint conta_pkey primary key (id)
);

create index idx_conta_numero_conta on conta (numero_conta);

create table transacao(
    id uuid not null,
    atualizado_em timestamptz(6) not null,
    conta_destino varchar(100) not null,
    criado_em timestamptz(6) not null,
    id_conta_origem uuid not null,
    id_correlacao uuid not null,
    id_idempotencia uuid null,
    moeda varchar(3) not null,
    status varchar(15) not null,
    tipo varchar(10) not null,
    valor numeric(15, 2) not null,
    versao int8 not null,
    constraint transacao_pkey primary key (id),
    constraint uq_transacao_id_idempotencia unique (id_idempotencia),
    constraint fk_transacao_conta_origem foreign key (id_conta_origem) references conta (id)
);

create index idx_transacao_id_correlacao on transacao (id_correlacao);
create index idx_transacao_id_idempotencia on transacao (id_idempotencia) where id_idempotencia is not null;

create table saldo(
    id uuid not null,
    atualizado_em timestamptz(6) not null,
    bloqueado numeric(15, 2) not null,
    disponivel numeric(15, 2) not null,
    id_conta uuid not null,
    versao int8 not null,
    constraint saldo_pkey primary key (id),
    constraint uq_saldo_id_conta unique (id_conta),
    constraint ck_saldo_disponivel_positivo check (disponivel >= 0),
    constraint ck_saldo_bloqueado_positivo check (bloqueado >= 0),
    constraint fk_saldo_conta foreign key (id_conta) references conta (id)
);

create table limite(
    id uuid not null,
    data_referencia date not null,
    id_conta uuid not null,
    limite_diario numeric(15, 2) not null,
    limite_utilizado numeric(15, 2) not null,
    tipo varchar(10) not null,
    utilizado_hoje numeric(15, 2) not null,
    constraint limite_pkey primary key (id),
    constraint fk_limite_conta foreign key (id_conta) references conta (id)
);

create index idx_limite_id_conta_tipo on limite (id_conta, tipo);