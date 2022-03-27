package sachin.com.flowable.common.transaction;

public interface TransactionContext {
    void commit();
    void rollback();

    void addTransactionListener(TransactionState state, TransactionListener listener);
}
