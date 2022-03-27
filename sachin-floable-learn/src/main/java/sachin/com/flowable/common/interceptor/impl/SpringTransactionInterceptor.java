package sachin.com.flowable.common.interceptor.impl;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.executor.CommandExecutor;
import sachin.com.flowable.common.interceptor.AbstractCommandInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public class SpringTransactionInterceptor  extends AbstractCommandInterceptor {

    protected PlatformTransactionManager transactionManager;

    public SpringTransactionInterceptor(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public <T> T execute(final CommandConfig config, final  Command<T> command, CommandExecutor commandExecutor) {



        // If the transaction is required (the other two options always need to go through the transactionTemplate),
        // the transactionTemplate is not used when the transaction is already active.
        // The reason for this is that the transactionTemplate try-catches exceptions and marks it as rollback.
        // Which will break nested service calls that go through the same stack of interceptors.

        /**
         * //如果事务是必需的(其他两个选项总是需要通过transactionTemplate)，
         * //当事务已经被激活时，transactionTemplate不被使用。
         * //这样做的原因是transactionTemplate尝试捕获异常并将其标记为回滚。
         * //这将打破嵌套的服务调用，通过相同的堆栈拦截器。
         *
         */
        int transactionPropagation = getPropagation(config);

        /**
         * 这里判断如果是需要事务，而且已经开启过了事务，则不需要再次开启事务，而是直接往下执行
         * 值得注意的是，在Spring中 如何判断当前是否存在事务 是通过  isActualTransactionActive方法来判断的，
         * 在TransactionSynchronizationManager中还有一个isSynchronizationActive 方法，这两个方法的语义不同。
         *
         *
         */
        if (transactionPropagation == TransactionTemplate.PROPAGATION_REQUIRED && TransactionSynchronizationManager.isActualTransactionActive()) {
            return next.execute(config, command, commandExecutor);
        }else{
            /**
             * 不不存在事务，则按照事务的要求级别 进行
             */
            TransactionTemplate transactionTemplate=new TransactionTemplate(transactionManager);
            /**
             *
             * 设置事务的传播行为：在Spring中，当一个方法调用另外一个方法时，可以让事务采取不同的策略工作，如新建事务或者挂起当前事务等，这便是事务的传播行为。
             *
             */
            transactionTemplate.setPropagationBehavior(getPropagation(config));
            T result = transactionTemplate.execute(new TransactionCallback<T>() {
                @Override
                public T doInTransaction(TransactionStatus transactionStatus) {

                    return next.execute(config, command,commandExecutor);
                }
            });

            return result;
        }

    }


    private int getPropagation(CommandConfig config){

        switch (config.getPropagation()) {
            case NOT_SUPPORTED:
                return TransactionTemplate.PROPAGATION_NOT_SUPPORTED;
            case REQUIRED:
                return TransactionTemplate.PROPAGATION_REQUIRED;
            case REQUIRES_NEW:
                return TransactionTemplate.PROPAGATION_REQUIRES_NEW;
            default:
                throw  new RuntimeException("not support");
        }

    }
}
