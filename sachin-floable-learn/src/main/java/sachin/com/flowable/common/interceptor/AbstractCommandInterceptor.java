package sachin.com.flowable.common.interceptor;

public abstract class AbstractCommandInterceptor  implements  CommandInterceptor{


    protected CommandInterceptor next;

    @Override
    public CommandInterceptor getNext() {
        return next;
    }

    @Override
    public void setNext(CommandInterceptor next) {
        this.next = next;
    }
}
