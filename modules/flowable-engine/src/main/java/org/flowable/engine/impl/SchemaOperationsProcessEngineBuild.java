/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl;

import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class SchemaOperationsProcessEngineBuild implements Command<Void> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaOperationsProcessEngineBuild.class);

    /**
     *
     * org.flowable.engine.impl.SchemaOperationsProcessEngineBuild.execute(org.flowable.common.engine.impl.interceptor.CommandContext)
     * org.flowable.engine.impl.interceptor.CommandInvoker$1.run()
     * org.flowable.engine.impl.interceptor.CommandInvoker.executeOperation(java.lang.Runnable)
     * org.flowable.engine.impl.interceptor.CommandInvoker.executeOperations(org.flowable.common.engine.impl.interceptor.CommandContext)
     * org.flowable.engine.impl.interceptor.CommandInvoker.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.engine.impl.interceptor.BpmnOverrideContextInterceptor.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.common.engine.impl.interceptor.TransactionContextInterceptor.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.common.engine.impl.interceptor.CommandContextInterceptor.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.idm.spring.SpringTransactionInterceptor$1.doInTransaction(org.springframework.transaction.TransactionStatus)
     * org.flowable.idm.spring.SpringTransactionInterceptor.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.common.engine.impl.interceptor.LogInterceptor.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.common.engine.impl.cfg.CommandExecutorImpl.execute(org.flowable.common.engine.impl.interceptor.CommandConfig, org.flowable.common.engine.impl.interceptor.Command)
     * org.flowable.engine.impl.ProcessEngineImpl.<init>(org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl)
     * org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl.buildProcessEngine()
     * org.flowable.spring.SpringProcessEngineConfiguration.buildProcessEngine()
     * org.flowable.spring.ProcessEngineFactoryBean.getObject()
     * org.flowable.spring.ProcessEngineFactoryBean.getObject()
     *
     *
     * @param commandContext
     * @return
     */
    @Override
    public Void execute(CommandContext commandContext) {
        
        SchemaManager schemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getSchemaManager();
        String databaseSchemaUpdate = CommandContextUtil.getProcessEngineConfiguration().getDatabaseSchemaUpdate();
        
        LOGGER.debug("Executing schema management with setting {}", databaseSchemaUpdate);
        if (ProcessEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
            try {
                schemaManager.schemaDrop();
            } catch (RuntimeException e) {
                // ignore
            }
        }
        if (ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate)
                || ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate) || ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_CREATE.equals(databaseSchemaUpdate)) {
            schemaManager.schemaCreate();

        } else if (ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
            schemaManager.schemaCheckVersion();

        } else if (ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
            schemaManager.schemaUpdate();
        }
        
        return null;
    }
}