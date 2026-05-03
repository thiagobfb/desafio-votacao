ALTER TABLE voto DROP CONSTRAINT uk_voto_pauta_associado;
ALTER TABLE voto RENAME COLUMN associado_id TO cpf;
ALTER TABLE voto ADD CONSTRAINT uk_voto_pauta_cpf UNIQUE (pauta_id, cpf);
