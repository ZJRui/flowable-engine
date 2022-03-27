package sachin.com.flowable.common.interceptor;


import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.executor.CommandExecutor;

public interface CommandInterceptor {
    <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor);

    CommandInterceptor getNext();

    void setNext(CommandInterceptor next);

}
