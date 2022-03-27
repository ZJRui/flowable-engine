package sachin.com.flowable.common.engine;

import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.cfg.CommandContextFactory;
import sachin.com.flowable.common.executor.CommandExecutor;
import sachin.com.flowable.common.executor.CommandExecutorImpl;
import sachin.com.flowable.common.interceptor.CommandInterceptor;
import sachin.com.flowable.common.interceptor.impl.CommandContextInterceptor;
import sachin.com.flowable.common.interceptor.impl.DefaultCommandInvoker;
import sachin.com.flowable.common.interceptor.impl.LogInterceptor;
import sachin.com.flowable.common.interceptor.impl.TransactionContextInterceptor;
import sachin.com.flowable.common.transaction.TransactionContextFactory;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class Engine {
    protected CommandConfig defaultCommandConfig;
    protected CommandContextFactory commandContextFactory;
    protected TransactionContextFactory transactionContextFactory;
    protected CommandInterceptor commandInvoker;
    protected List<CommandInterceptor> commandInterceptors;
    protected Collection<? extends CommandInterceptor> defaultCommandInterceptors;
    protected ClassLoader classLoader;
    /**
     * 这个CommandExecutor如何被使用呢？
     *
     */
    protected CommandExecutor commandExecutor;
    /**
     * Either use Class.forName or ClassLoader.loadClass for class loading. See http://forums.activiti.org/content/reflectutilloadclass-and-custom- classloader
     */
    protected boolean useClassForNameClassLoading = true;

    public void initDefaultCommandConfig() {
        if (defaultCommandConfig == null) {
            defaultCommandConfig = new CommandConfig();
        }
    }

    public void initCommandInvoker() {
        if (this.commandInvoker == null) {
            commandInvoker = new DefaultCommandInvoker();
        }
    }

    public void initCommandInterceptors() {
        if (commandInterceptors == null) {
            commandInterceptors = new ArrayList<>();
            commandInterceptors.addAll(getDefaultCommandInterceptors());
        }
    }

    public Collection<? extends CommandInterceptor> getDefaultCommandInterceptors() {
        if (defaultCommandInterceptors == null) {
            List<CommandInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new LogInterceptor());
            CommandInterceptor transactionInterceptor = createTransactionInterceptor();
            if (transactionInterceptor != null) {
                interceptors.add(transactionInterceptor);
            }

            if (commandContextFactory != null) {
                CommandContextInterceptor commandContextInterceptor = new CommandContextInterceptor(commandContextFactory, classLoader, useClassForNameClassLoading);
                interceptors.add(commandContextInterceptor);

            }
            if (transactionContextFactory != null) {
                interceptors.add(new TransactionContextInterceptor(transactionContextFactory));
            }
            defaultCommandInterceptors = interceptors;
        }
        return defaultCommandInterceptors;
    }

    public void initCommandExecutor(){
        if (commandExecutor == null) {
            CommandInterceptor first = initInterceptorChain(commandInterceptors);
            commandExecutor = new CommandExecutorImpl(getDefaultCommandConfig(), first);
        }
    }

    protected CommandInterceptor initInterceptorChain(List<CommandInterceptor> chain) {
        if (chain == null || chain.isEmpty()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < chain.size()-1; i++) {
            chain.get(i).setNext(chain.get(i+1));
        }
        return chain.get(0);
    }
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }
}
