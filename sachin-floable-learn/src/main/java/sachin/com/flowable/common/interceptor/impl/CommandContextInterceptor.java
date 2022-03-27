package sachin.com.flowable.common.interceptor.impl;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.cfg.CommandConfig;
import sachin.com.flowable.common.cfg.CommandContext;
import sachin.com.flowable.common.cfg.CommandContextFactory;
import sachin.com.flowable.common.cfg.Context;
import sachin.com.flowable.common.exception.FlowableException;
import sachin.com.flowable.common.executor.CommandExecutor;
import sachin.com.flowable.common.interceptor.AbstractCommandInterceptor;

public class CommandContextInterceptor extends AbstractCommandInterceptor {


    protected CommandContextFactory commandContextFactory;
    protected ClassLoader classLoader;

    protected boolean useClassForNameClassLoading;

    public CommandContextInterceptor() {

    }

    public CommandContextInterceptor(CommandContextFactory contextFactory, ClassLoader classLoader, boolean useClassForNameClassLoading) {
        this.commandContextFactory = contextFactory;
        this.classLoader = classLoader;
        this.useClassForNameClassLoading = useClassForNameClassLoading;
    }

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
         *
         *
         *
         * 1,如果reusePossible 为false，表示不可重用； commandContext为null，一般表示第一次； 或者存在异常
         *
         * Interceptor拦截到要执行 指定的command，首先他从线程中拿到之前的CommandContext；
         * 如果我们的config中配置了context不允许重新使用，则我们会创建一个新的CommandContext
         * 又或者当前线程中还没有CommandContext，比如当前的interceptor是第一个Interceptor，则我们也会创建一个CommandContext
         * 又或者当前线程中的Context存在异常，则我们也会重新创建一个CommandContext
         *
         */

        if (!config.isContextReusePossible() || commandContext == null || commandContext.getException() != null) {

            /**
             * createCommandContext方法中总是new ，因此CommandContext不会多线程共享
             */
            commandContext = commandContextFactory.createCommandContext(command);
            commandContext.setCommandExecutor(commandExecutor);
            commandContext.setClassLoader(classLoader);
            commandContext.setUseClassForNameClassLoading(useClassForNameClassLoading);
        } else {
            /**
             *标记当前CommandContext是否被重用，用于后期清理工作
             */
            contextReused = true;
            /**
             * 获取context中是否被重复使用了的标记
             */
            originalContextReusedState = commandContext.isReused();
            /**
             * 将context设置为 重复使用标记
             */
            commandContext.setReused(true);
        }
        try {
            /**
             *将CommandContext放入到线程栈中,从这里我们看出来不管该CommandContext是新创建的还是复用了当前线程中的CommandContext，这里都会
             * 将这个CommandContext再次设置到线程中
             */
            Context.setCommandContext(commandContext);
            return next.execute(config, command, commandExecutor);
        } catch (Exception e) {
            commandContext.setException(e);
        }finally {
            try{
                if(!contextReused){
                    /**
                     * contextReused为false，意味着 这个CommandContext是当前Interceptor创建的，因此在finally中药执行关闭
                     */
                    commandContext.close();
                }
                /**
                 * 将CommandContext的是否被重复使用状态复原
                 */
                commandContext.setReused(originalContextReusedState);
            }finally {
                /**
                 * 问题：如果CommandContext被重用了，比如BInterceptor重用了A的CommandContext，为什么在B的Interceptor中还要removeCommandContext？
                 *
                 * 这是因为不管CommandContext是新创建还是 当前线程中已经存在的commanContext而重用的，在当前Interceptor中都会被 再次放入到线程栈中。也就是上面的try 中 的context.setCommandContext
                 *
                 * 因此这里需要remove
                 */
                Context.removeCommandContext();
            }
        }

        /**
         *
         * 注意： 在上面的try中 我们执行了   return next.execute(config, command, commandExecutor);
         * 也就是说正常情况下不抛出异常，则会执行上面的finally之后 就结束了。
         *
         * 如果程序能够执行到这个地方，则表示  next.execute过程中出现了异常，catch住了，执行了finally 但是没有执行return
         *
         *如果需要，重新抛出异常
         * 如果context是被重用的，  即便上面的finally中使用了Context.removeCommandContext,
         * 该contextCommand仍然存在于当前线程中，因为该CommandContext在之前的Interceptor中会被设置到线程中一次
         *
         * 问题：为什么这里需要判断contextReused？
         * 首先我们要明白，上面的try中如果 next.execute 出现了异常，将会导致执行catch，这个异常被catch住并放入到了CommandContext中，后面的finally中
         * 并没有处理这个异常，然后会执行到这个地方。  如果contextReused为true，表示当前Interceptor复用了线程中的CommandContext，为了不影响该CommandContext的实际
         * 所属Interceptor，这里需要对CommandContext中的exception进行清空处理。也就是下面的首先拿到exception，然后reset，那么当前Interceptor如何处理这个异常呢？
         * 直接转为FlowableException抛出。
         *
         * 如果ComandContext不是复用的 也就是contextReused为false，这个时候如果执行next.execute 出现了异常，将会导致下面执行return null
         *
         *
         */
        if (contextReused && commandContext.getException() != null) {
            /**
             *
             *       // If it's reused, we need to throw the exception again so it propagates upwards,
             *             // but the exception needs to be reset again or the parent call can incorrectly be marked
             *             // as having an exception (the nested call can be try-catched for example)
             *             //如果它被重用，我们需要再次抛出异常，以便它向上传播，
             * //但需要再次重置异常，否则可能会错误地标记父调用
             * //作为一个异常(例如，嵌套调用可以尝试捕获)
             *
             */
            Throwable exception = commandContext.getException();
            commandContext.resetException();
            // Wrapping it to avoid having 'throws throwable' in all method signatures
            //将其包装以避免在所有方法签名中使用'throws throwable'
            if (exception instanceof FlowableException) {
                throw (FlowableException) exception;
            }else {
                throw new FlowableException();
            }

        }
        return null;
    }
}
