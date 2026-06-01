create table outbox_evento (
    id uuid not null,
    tipo_evento varchar(100) not null,
    tipo_agregado varchar(50) not null,
    id_agregado uuid not null,
    topico varchar(120) not null,
    chave varchar(120) not null,
    payload jsonb not null,
    id_correlacao uuid not null,
    ocorrido_em timestamptz(6) not null,
    criado_em timestamptz(6) not null,
    publicado_em timestamptz(6),
    status varchar(20) not null,
    tentativas int not null default 0,
    ultimo_erro text,
    proxima_tentativa_em timestamptz(6) not null,
    constraint outbox_evento_pkey primary key (id),
    constraint chk_tentativas_positivo check (tentativas >= 0)
);

create index idx_outbox_evento_status_proxima_tentativa on outbox_evento (status, proxima_tentativa_em, criado_em);
create index idx_outbox_evento_topico on outbox_evento (topico, criado_em);
create index idx_outbox_evento_agregado on outbox_evento (tipo_agregado, id_agregado);

create table evento_processado (
    id uuid not null,
    id_evento uuid not null,
    grupo_consumidor varchar(120) not null,
    topico varchar(120) not null,
    id_correlacao uuid not null,
    processado_em timestamptz(6) not null,
    constraint evento_processado_pkey primary key (id),
    constraint uq_evento_processado_evento_grupo unique (id_evento, grupo_consumidor)
);

create index idx_evento_processado_grupo on evento_processado (grupo_consumidor, processado_em desc);
create index idx_evento_processado_id_correlacao on evento_processado (id_correlacao);