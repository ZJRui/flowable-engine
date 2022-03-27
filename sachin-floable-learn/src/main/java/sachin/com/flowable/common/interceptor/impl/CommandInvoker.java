package sachin.com.flowable.common.interceptor.impl;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.cfg.CommandContext;
import sachin.com.flowable.common.cfg.Context;
import sachin.com.flowable.common.executor.CommandExecutor;
import sachin.com.flowable.common.interceptor.AbstractCommandInterceptor;

public class CommandInvoker extends AbstractCommandInterceptor {


    @Override
    public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {


        CommandContext commandContext = Context.getCommandContext();


        return command.execute(commandContext);
    }
}
