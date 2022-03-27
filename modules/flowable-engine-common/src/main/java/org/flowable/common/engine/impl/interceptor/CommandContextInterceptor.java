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

package org.flowable.common.engine.impl.interceptor;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.runtime.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class CommandContextInterceptor extends AbstractCommandInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandContextInterceptor.class);

    protected CommandContextFactory commandContextFactory;
    protected ClassLoader classLoader;
    protected boolean useClassForNameClassLoading;
    protected Clock clock;
    protected ObjectMapper objectMapper;
    protected Map<String, AbstractEngineConfiguration> engineConfigurations = new HashMap<>();

    protected String engineCfgKey;

    public CommandContextInterceptor() {
    }

    public CommandContextInterceptor(CommandContextFactory commandContextFactory, ClassLoader classLoader,
                                     boolean useClassForNameClassLoading, Clock clock, ObjectMapper objectMapper) {

        this.commandContextFactory = commandContextFactory;
        this.classLoader = classLoader;
        this.useClassForNameClassLoading = useClassForNameClassLoading;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    /**
     * 顺序
     * CommandExecutorImpl对象中的first属性 LogInterceptor
     * SpringTransactionInterceptor
     * CommandContextInterceptor
     * TransactionContextInterceptor
     * BpmnOverrideContextInterceptor
     * CommandInvoker
     *
     *
     *
     * @param config
     * @param command
     * @param commandExecutor
     * @param <T>
     * @return
     */

    @Override
    public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {
        CommandContext commandContext = Context.getCommandContext();

        /*
         * This flag indicates whether the context is reused for the execution of the current command.
         * If a valid command context exists, this means a nested service call is being executed.
         * If so, this flag will change to 'true', with the purpose of closing the command context in the finally block.
         *
         * *该标志指示当前命令执行时上下文是否被重用。
         *如果存在有效的命令上下文，这意味着正在执行嵌套的服务调用。
         *如果是，该标志将更改为'true'，目的是在finally块中关闭命令上下文。
         */
        boolean contextReused = false;

        /*
         * Commands can execute service calls, even deeply nested service calls.
         * This flag stores the 'reused' flag on the command context as it was when starting to execute the command.
         * For a nested command, this will be 'true'. Only for the root command context usage, this will be false.
         * When the nested command is done, the original state is restored, which allows to detect at the CommandInvoker
         * level which command context is the actual root.
         *
         * *命令可以执行服务调用，甚至是深度嵌套的服务调用。
*这个标志在命令上下文中存储'reuse '标志，就像它在开始执行命令时一样。
*对于嵌套的命令，这将是'true'。仅在根命令上下文使用时，此值将为false。
*当嵌套命令完成时，恢复原来的状态，这允许在CommandInvoker检测
命令上下文是实际的根。
         */
        boolean originalContextReusedState = false;

        // We need to check the exception, because the transaction can be in a
        // rollback state, and some other command is being fired to compensate (eg. decrementing job retries)
        /**
         * //我们需要检查异常，因为事务可以在
         * //回滚状态，并且一些其他的命令正在被触发以补偿(例如。递减工作重试)
         */
        if (!config.isContextReusePossible() || commandContext == null || commandContext.getException() != null) {
            commandContext = commandContextFactory.createCommandContext(command);
            commandContext.setEngineConfigurations(engineConfigurations);
            commandContext.setCommandExecutor(commandExecutor);
            commandContext.setClassLoader(classLoader);
            commandContext.setUseClassForNameClassLoading(useClassForNameClassLoading);
            commandContext.setClock(clock);
            commandContext.setObjectMapper(objectMapper);

        } else {
            LOGGER.debug("Valid context found. Reusing it for the current command '{}'", command.getClass().getCanonicalName());
            contextReused = true;
            originalContextReusedState = commandContext.isReused();
            commandContext.setReused(true);
        }


        try {
            // Push the current engine configuration key to a stack
            // shared between nested calls that reuse the command context
            commandContext.pushEngineCfgToStack(engineCfgKey);

            // Push on command stack
            Context.setCommandContext(commandContext);

            return next.execute(config, command, commandExecutor);

        } catch (Exception e) {

            commandContext.exception(e);

        } finally {
            try {
                if (!contextReused) {
                    commandContext.close();
                }
                commandContext.setReused(originalContextReusedState);

            } finally {
                // Pop from stacks
                commandContext.popEngineCfgStack();
                Context.removeCommandContext();
            }
        }

        // Rethrow exception if needed
        if (contextReused && commandContext.getException() != null) {

            // If it's reused, we need to throw the exception again so it propagates upwards,
            // but the exception needs to be reset again or the parent call can incorrectly be marked
            // as having an exception (the nested call can be try-catched for example)
            /**
             * //如果它被重用，我们需要再次抛出异常，以便它向上传播，
             * //但需要再次重置异常，否则可能会错误地标记父调用
             * //作为一个异常(例如，嵌套调用可以尝试捕获)
             */
            Throwable exception = commandContext.getException();
            commandContext.resetException();

            // Wrapping it to avoid having 'throws throwable' in all method signatures
            //将其包装以避免在所有方法签名中使用'throws throwable'
            if (exception instanceof FlowableException) {
                throw (FlowableException) exception;
            } else {
                throw new FlowableException("Exception during command execution", exception);
            }
        }

        return null;
    }

    public CommandContextFactory getCommandContextFactory() {
        return commandContextFactory;
    }

    public void setCommandContextFactory(CommandContextFactory commandContextFactory) {
        this.commandContextFactory = commandContextFactory;
    }

    public Map<String, AbstractEngineConfiguration> getEngineConfigurations() {
        return engineConfigurations;
    }

    public void setEngineConfigurations(Map<String, AbstractEngineConfiguration> engineConfigurations) {
        this.engineConfigurations = engineConfigurations;
    }

    public String getEngineCfgKey() {
        return engineCfgKey;
    }
    public void setEngineCfgKey(String engineCfgKey) {
        this.engineCfgKey = engineCfgKey;
    }
}
