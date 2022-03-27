package sachin.com.flowable.common.transaction;

import sachin.com.flowable.common.cfg.CommandContext;

public interface TransactionListener {
    void execute(CommandContext commandContext);
}
