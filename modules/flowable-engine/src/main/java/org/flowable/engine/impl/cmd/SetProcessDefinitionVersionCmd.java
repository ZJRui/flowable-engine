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

package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * {@link Command} that changes the process definition version of an existing process instance.
 * 
 * Warning: This command will NOT perform any migration magic and simply set the process definition version in the database, assuming that the user knows, what he or she is doing.
 * 
 * This is only useful for simple migrations. The new process definition MUST have the exact same activity id to make it still run.
 * 
 * Furthermore, activities referenced by sub-executions and jobs that belong to the process instance MUST exist in the new process definition version.
 * 
 * The command will fail, if there is already a {@link ProcessInstance} or {@link HistoricProcessInstance} using the new process definition version and the same business key as the
 * {@link ProcessInstance} that is to be migrated.
 * 
 * If the process instance is not currently waiting but actively running, then this would be a case for optimistic locking, meaning either the version update or the "real work" wins, i.e., this is a
 * race condition.
 * 
 * @see <a href="http://forums.activiti.org/en/viewtopic.php?t=2918">http://forums.activiti.org/en/viewtopic.php?t=2918</a>
 * @author Falko Menge
 */
public class SetProcessDefinitionVersionCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String processInstanceId;
    private final Integer processDefinitionVersion;

    /**
     * 流程实例的id和 流程版本
     * @param processInstanceId
     * @param processDefinitionVersion
     */
    public SetProcessDefinitionVersionCmd(String processInstanceId, Integer processDefinitionVersion) {
        if (processInstanceId == null || processInstanceId.length() < 1) {
            throw new FlowableIllegalArgumentException("The process instance id is mandatory, but '" + processInstanceId + "' has been provided.");
        }
        if (processDefinitionVersion == null) {
            throw new FlowableIllegalArgumentException("The process definition version is mandatory, but 'null' has been provided.");
        }
        if (processDefinitionVersion < 1) {
            throw new FlowableIllegalArgumentException("The process definition version must be positive, but '" + processDefinitionVersion + "' has been provided.");
        }
        this.processInstanceId = processInstanceId;
        this.processDefinitionVersion = processDefinitionVersion;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        // check that the new process definition is just another version of the same
        // process definition that the process instance is using
        ExecutionEntityManager executionManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity processInstance = executionManager.findById(processInstanceId);
        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("No process instance found for id = '" + processInstanceId + "'.", ProcessInstance.class);
        } else if (!processInstance.isProcessInstanceType()) {
            throw new FlowableIllegalArgumentException("A process instance id is required, but the provided id " + "'" + processInstanceId + "' " + "points to a child execution of process instance " + "'"
                    + processInstance.getProcessInstanceId() + "'. " + "Please invoke the " + getClass().getSimpleName() + " with a root execution id.");
        }

        DeploymentManager deploymentCache = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDeploymentManager();
        //根据流程实例 找到该流程实例的流程定义
        ProcessDefinition currentProcessDefinition = deploymentCache.findDeployedProcessDefinitionById(processInstance.getProcessDefinitionId());

        /**
         *
         *一个流程定义是一个ProcessDefinition对象，在金刚中，更新同一个流程，会重新创建一个WorkflowDefinition对象，这个对象中有一个sourceId属性，
         * sourceId属性的值等于该流程创建时第一个workflowDefinition对象的 oid，然后更新流程的时候会重新生成后一个workflowDefinition，同一个流程的所有的 workflowDefinition的sourceId都是相同的
         * 然后根据workflowDefinition生成xml文件时  <process id="process_233521904" name="wnq_workflowTest01流程优化002" isExecutable="true"></process>
         * 这个id就是process_sourceid，因此相同流程 生成的xml文件的 process节点的id是相同的.
         * 在 org.flowable.engine.impl.bpmn.parser.handler.ProcessParseHandler#transformProcess(org.flowable.engine.impl.bpmn.parser.BpmnParse, org.flowable.bpmn.model.Process) 中对process
         * 节点进行处理，在处理过程中我们看到 process的id作为  currentProcessDefinition.setKey(process.getId()); key。
         *ProcessDefinitionEntity对象对应的mybatis文件是ProcessDefinition.xml，从这个文件中我们可以看到 ProcessDefinition对应 的是ACT_RE_PROCDEF表。
         *
         * 也就是说： 流程部署xml
         *  //部署流程
         *         Deployment deployment = repositoryService.createDeployment().addString(workflowName + ".bpmn20.xml", bpmnSource).deploy();
         *         会首先在 act_re_deploymnet表中生成一个Deployment对象，然后 在部署的过程中会解析xml创建后一个ProcessDefinition对象保存到act_re_procdef表
         *
         *    在这里findDeployedProcessDefinitionByKeyAndVersionAndTenantId 会根究act_re_procdef表中的key 和流程的版本信息获取到 ProcessDefinition定义。
         *
         */

        ProcessDefinition newProcessDefinition = deploymentCache
                .findDeployedProcessDefinitionByKeyAndVersionAndTenantId(currentProcessDefinition.getKey(), processDefinitionVersion, currentProcessDefinition.getTenantId());

        if (Flowable5Util.isFlowable5ProcessDefinition(currentProcessDefinition, commandContext) && !Flowable5Util
            .isFlowable5ProcessDefinition(newProcessDefinition, commandContext)) {
            throw new FlowableIllegalArgumentException("The current process definition (id = '" + currentProcessDefinition.getId() + "') is a v5 definition."
                + " However the new process definition (id = '" + newProcessDefinition.getId() + "') is not a v5 definition.");
        }

        validateAndSwitchVersionOfExecution(commandContext, processInstance, newProcessDefinition);

        // switch the historic process instance to the new process definition version
        CommandContextUtil.getHistoryManager(commandContext).recordProcessDefinitionChange(processInstanceId, newProcessDefinition.getId());

        // switch all sub-executions of the process instance to the new process definition version
        Collection<ExecutionEntity> childExecutions = executionManager.findChildExecutionsByProcessInstanceId(processInstanceId);
        for (ExecutionEntity executionEntity : childExecutions) {
            validateAndSwitchVersionOfExecution(commandContext, executionEntity, newProcessDefinition);
        }

        return null;
    }

    protected void validateAndSwitchVersionOfExecution(CommandContext commandContext, ExecutionEntity execution, ProcessDefinition newProcessDefinition) {
        // check that the new process definition version contains the current activity
        org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(newProcessDefinition.getId());
        if (execution.getActivityId() != null && process.getFlowElement(execution.getActivityId(), true) == null) {
            throw new FlowableException("The new process definition " + "(key = '" + newProcessDefinition.getKey() + "') " + "does not contain the current activity " + "(id = '"
                    + execution.getActivityId() + "') " + "of the process instance " + "(id = '" + processInstanceId + "').");
        }

        // switch the process instance to the new process definition version
        execution.setProcessDefinitionId(newProcessDefinition.getId());
        execution.setProcessDefinitionName(newProcessDefinition.getName());
        execution.setProcessDefinitionKey(newProcessDefinition.getKey());

        // and change possible existing tasks (as the process definition id is stored there too)
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        List<TaskEntity> tasks = processEngineConfiguration.getTaskServiceConfiguration().getTaskService().findTasksByExecutionId(execution.getId());
        Clock clock = processEngineConfiguration.getClock();
        for (TaskEntity taskEntity : tasks) {
            taskEntity.setProcessDefinitionId(newProcessDefinition.getId());
            processEngineConfiguration.getActivityInstanceEntityManager().recordTaskInfoChange(taskEntity, clock.getCurrentTime());
        }
    }

}
