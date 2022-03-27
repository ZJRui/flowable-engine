package sachin.com.flowable.common.cfg;

public interface CommandContextCloseListener {

    /**
     * Called when the CommandContext is being closed, but no 'close logic' has been executed. At this point,
     * the TransactionContext (if applicable) has not yet been committed/rolledback and none of the Session instances
     * have been flushed. If an exception happens and it is not caught in this method: -
     * The Session instances will *not* be flushed - The TransactionContext will be rolled back (if applicable)
     *
     * 当CommandContext被关闭时调用，但是没有'关闭逻辑'被执行。此时，TransactionContext(如果适用)还没有被提交/回滚，并且没有一个会话实例被刷新。如果一个异常发生了，并且它没有在这个方法中被捕获:
     * @param commandContext
     */
    void closing(CommandContext commandContext);

    /**
     *
     * Called when the Session have been successfully flushed.
     * When an exception happened during the flushing of the sessions, this method will not be called.
     * If an exception happens and it is not caught in this method: - The Session instances will *not* be flushed - The TransactionContext will be rolled back (if applicable)
     *
     * 成功刷新会话时调用。当刷新会话期间发生异常时，将不会调用此方法。如果一个异常发生了，并且它没有在这个方法中被捕获:
     *
     * @param commandContext
     */
    void afterSessionFlush(CommandContext commandContext);

    /**
     *Called when the CommandContext is successfully closed. At this point, the TransactionContext (if applicable)
     *  has been successfully committed and no rollback has happened. All Session instances have been closed.
     *  Note that throwing an exception here does *not* affect the transaction. The CommandContext will log the exception though.
     *
     *当CommandContext成功关闭时调用。此时，TransactionContext(如果适用)已经成功提交，并且没有发生回滚。所有Session实例已被关闭。注意，在这里抛出异常并不会影响事务。CommandContext将记录异常。
     * @param commandContext
     */
    void closed(CommandContext commandContext);

    /**
     * Called when the CommandContext has not been successfully closed due to an exception that
     * happened. Note that throwing an exception here does *not* affect the transaction. The CommandContext will log the exception though.
     *
     * 当由于发生异常而无法成功关闭CommandContext时调用。注意，在这里抛出异常并不会影响事务。CommandContext将记录异常。
     *
     * @param commandContext
     */
    void closeFailure(CommandContext commandContext);

    Integer order();

    /**
     * Determines if there are multiple occurrences allowed of this close listener
     * 确定是否允许此关闭侦听器多次出现
     * @return
     */
    boolean multipleAllowed();
}

