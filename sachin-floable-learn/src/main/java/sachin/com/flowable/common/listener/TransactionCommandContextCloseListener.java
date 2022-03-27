package sachin.com.flowable.common.listener;

import sachin.com.flowable.common.cfg.CommandContext;
import sachin.com.flowable.common.cfg.CommandContextCloseListener;
import sachin.com.flowable.common.transaction.TransactionContext;

public class TransactionCommandContextCloseListener implements CommandContextCloseListener {

    protected TransactionContext transactionContext;

    public TransactionCommandContextCloseListener(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    @Override
    public void closing(CommandContext commandContext) {


    }

    @Override
    public void afterSessionFlush(CommandContext commandContext) {
        transactionContext.commit();

    }

    @Override

    public void closed(CommandContext commandContext) {

    }

    @Override
    public void closeFailure(CommandContext commandContext) {

        transactionContext.rollback();

    }

    @Override
    public Integer order() {
        return 10000;
    }

    @Override
    public boolean multipleAllowed() {
        return false;
    }
}
