package sachin.com.flowable.common.interceptor.impl;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.cfg.CommandContext;
import sachin.com.flowable.common.cfg.Context;
import sachin.com.flowable.common.executor.CommandExecutor;
import sachin.com.flowable.common.interceptor.AbstractCommandInterceptor;
import sachin.com.flowable.common.interceptor.CommandInterceptor;

public class DefaultCommandInvoker extends AbstractCommandInterceptor {
    @Override
    public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {


        //第一种写法，将command的返回值直接返回
//        final CommandContext commandContext = Context.getCommandContext();
//        T result = command.execute(commandContext);
//
//        return result;


        //第二种写法，将Command的返回值设置到CommandContext中，然后通过commandContext的getResult方法返回
        final CommandContext commandContext = Context.getCommandContext();
        commandContext.setResult(command.execute(commandContext));

        //第二中写法的主要原因是，某些CommandInvoker，比如示例中的AppCommandInvoker 中存在其他方法，其他方法接受的对象是ComandContext
        // ，而不是command，因此我们将结果设置到CommandContext中了

        return (T) commandContext.getResult();
    }

    @Override
    public void setNext(CommandInterceptor next) {
        throw new UnsupportedOperationException("CommandInvoker must be the last interceprot in the chain");
    }
}
