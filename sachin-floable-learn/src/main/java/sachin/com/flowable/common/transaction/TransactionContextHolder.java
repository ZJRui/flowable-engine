package sachin.com.flowable.common.transaction;

import java.util.Stack;

public class TransactionContextHolder {


    protected static ThreadLocal<Stack<TransactionContext>> transactionContextThreadLocal = new ThreadLocal<>();

    public  static TransactionContext getTransactionContext(){


        Stack<TransactionContext> stack = getStack(transactionContextThreadLocal);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peek();


    }

    public static void setTransactionContext(TransactionContext transactionContext) {

        getStack(transactionContextThreadLocal).push(transactionContext);
    }
    public static  void removeTransactionContext(){

        getStack(transactionContextThreadLocal).pop();
    }
    public static  boolean isTransactionContextActive(){
        return !getStack(transactionContextThreadLocal).isEmpty();
    }
    protected static <T> Stack<T> getStack(ThreadLocal<Stack<T>> threadLocal) {
        Stack<T> stack = threadLocal.get();
        if (stack == null) {
            stack = new Stack<>();
            threadLocal.set(stack);
        }
        return stack;
    }
}
