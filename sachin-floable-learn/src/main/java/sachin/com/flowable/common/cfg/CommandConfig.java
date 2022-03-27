package sachin.com.flowable.common.cfg;

import sachin.com.flowable.common.cfg.TransactionPropagation;

/**
 * Configuration settings for the command interceptor chain.
 *
 * Instances of this class are immutable, and thus thread- and share-safe.
 *
 * *命令拦截器链的配置。
 * *该类的实例是不可变的，因此线程和共享安全。
 */
public class CommandConfig {


    private boolean contextReusePossible;

    private TransactionPropagation propagation;

    public CommandConfig(){
        this.contextReusePossible=true;
        this.propagation=TransactionPropagation.REQUIRED;
    }
    public CommandConfig(boolean contextReusePossible){
        this.contextReusePossible=contextReusePossible;
        this.propagation=TransactionPropagation.REQUIRED;

    }
    public CommandConfig(boolean contextReusePossible, TransactionPropagation propagation){
        this.contextReusePossible=contextReusePossible;
        this.propagation=propagation;
    }

    public boolean isContextReusePossible() {
        return contextReusePossible;
    }

    public void setContextReusePossible(boolean contextReusePossible) {
        this.contextReusePossible = contextReusePossible;
    }

    public TransactionPropagation getPropagation() {
        return propagation;
    }

    public void setPropagation(TransactionPropagation propagation) {
        this.propagation = propagation;
    }
}
