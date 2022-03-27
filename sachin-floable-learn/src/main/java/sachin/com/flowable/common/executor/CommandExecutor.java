package sachin.com.flowable.common.executor;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;

public  interface CommandExecutor {


    CommandConfig getDefaultConfig();

    <T> T execute(CommandConfig config, Command<T> command);

    <T> T execute(Command<T> command);

}
