package sachin.com.flowable.common.executor;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.interceptor.CommandInterceptor;

public class CommandExecutorImpl  implements CommandExecutor {

    protected CommandConfig defaultConfig;

    protected CommandInterceptor first;

    public CommandExecutorImpl(CommandConfig defaultConfig,CommandInterceptor first){

        this.defaultConfig=defaultConfig;
        this.first=first;
    }

    @Override
    public CommandConfig getDefaultConfig() {
        return  this.defaultConfig;
    }

    @Override
    public <T> T execute(CommandConfig config, Command<T> command) {

        /**
         * 当我们将Command交给CommandExecutor去执行的时候，实际上CommandExecutor内部是交给了CommandInterceptor去执行
         *
         * 同时CommandExecutor将自身引用通过 CommandInterceptor的execute方法交给 CommandInterceptor方法，
         *
         * 同时每一个CommandInterceptor中存在next属性，该属性时对下一个CommandInterceptor的引用。
         *
         * 因此command最终会被交给最后一个CommandInterceptor
         *
         */
        return  first.execute(config,command,this);
    }

    @Override
    public <T> T execute(Command<T> command) {
        return execute(this.defaultConfig,command);
    }
}
