alter table ACT_HI_PROCINST add column PROPAGATED_STAGE_INST_ID_ varchar(255);

create index ACT_IDX_EXEC_REF_ID_ on ACT_RU_EXECUTION(REFERENCE_ID_);
create index ACT_IDX_RU_ACTI_TASK on ACT_RU_ACTINST(TASK_ID_);

update ACT_GE_PROPERTY set VALUE_ = '6.7.0.0' where NAME_ = 'schema.version';
