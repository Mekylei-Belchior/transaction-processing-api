create table auditoria (
    id uuid not null,
    id_operador uuid not null,
    acao varchar(50) not null,
    recurso varchar(50) not null,
    id_recurso uuid,
    id_correlacao uuid not null,
    dados_anteriores jsonb,
    dados_novos jsonb,
    ip_origem varchar(45),
    ocorrido_em timestamptz not null,
    constraint auditoria_pkey primary key (id)
);

-- Sem UPDATE e DELETE por convenção — enforced na aplicação via updatable = false
create rule auditoria_no_update as on update to auditoria do instead nothing;
create rule auditoria_no_delete as on delete to auditoria do instead nothing;

-- Índices para consultas
create index idx_auditoria_recurso_id_recurso on auditoria (recurso, id_recurso, ocorrido_em desc);
create index idx_auditoria_id_operador on auditoria (id_operador, ocorrido_em desc);
create index idx_auditoria_id_correlacao on auditoria (id_correlacao);
create index idx_auditoria_ocorrido_em on auditoria (ocorrido_em desc);