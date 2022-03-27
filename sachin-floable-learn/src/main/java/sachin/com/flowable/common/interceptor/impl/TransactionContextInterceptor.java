package sachin.com.flowable.common.interceptor.impl;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.cfg.CommandContext;
import sachin.com.flowable.common.cfg.Context;
import sachin.com.flowable.common.cfg.TransactionPropagation;
import sachin.com.flowable.common.executor.CommandExecutor;
import sachin.com.flowable.common.interceptor.AbstractCommandInterceptor;
import sachin.com.flowable.common.listener.TransactionCommandContextCloseListener;
import sachin.com.flowable.common.transaction.TransactionContext;
import sachin.com.flowable.common.transaction.TransactionContextFactory;
import lombok.Data;

@Data
public class TransactionContextInterceptor extends AbstractCommandInterceptor {

    protected TransactionContextFactory transactionContextFactory;

    public TransactionContextInterceptor() {

    }

    public TransactionContextInterceptor(TransactionContextFactory transactionContextFactory) {
        this.transactionContextFactory = transactionContextFactory;
    }

    @Override
    public <T> T execute(CommandConfig config, Command<T> command, CommandExecutor commandExecutor) {

        CommandContext commandContext = Context.getCommandContext();
        /**
         * 将其存储在一个变量中，以便以后引用(它可以在命令执行期间更改)
         *
         * Q1:commandContext 会不会为null， 不会的，因为TransactionContextInterceptor之前会有CommandContextInterceptor,
         * 这个interceptor 会保证线程中一定存在CommandContext
         *
         * Q2: TransactionContextInterceptor的主要作用是 不是开启事务，而是创建TransactionContextInterceptor放置到线程中
         *
         * Q3:LogInterceptor ->SpringTransactionInterceptor-->CommandContextInerceptor--> TransactionContextInterceptor
         *
         * Q4：为什么openTransaction属性需要取决于 commandContext.isReused
         *
         * 如果commandContext中的isReused为true，表示他是一个复用的CommandContext，则openTransaction为false，表示不创建TransactionContext
         *
         * 如果CommandContext中的isReused为false，表示他不是一个复用的CommandContext，则就是新创建的CommandContext，则openTransaction为true，表示创建TransactionContext
         *
         *
         *
         */
        boolean openTransaction = !config.getPropagation().equals(TransactionPropagation.NOT_SUPPORTED)
                && transactionContextFactory != null && !commandContext.isReused();
        boolean isContextSet = false;

        try {
            if (openTransaction) {
                TransactionContext transactionContext = transactionContextFactory.openTransactionContext(commandContext);
                Context.setTransactionContext(transactionContext);
                isContextSet = true;
                commandContext.addCloseListener(new TransactionCommandContextCloseListener(transactionContext));
            }
            return next.execute(config, command, commandExecutor);
        } finally {
            if (openTransaction && isContextSet) {
                Context.removeCommandContext();

            }
        }

    }
}
