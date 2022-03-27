package sachin.com.flowable.common.cfg;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.exception.FlowableException;
import sachin.com.flowable.common.executor.CommandExecutor;
import  sachin.com.flowable.common.session.Session;
import sachin.com.flowable.common.session.SessionFactory;
import lombok.Data;

import java.util.*;

@Data
public class CommandContext {


    protected Command<?> command;
    /**
     * 虽然创建CommandContext只需要CommandConfig，在CommandContextInterceptor中，在创建CommandContext之后
     * 会调用   commandContext.setCommandExecutor(commandExecutor); 将CommandExecutor设置给CommandContext
     *
     */
    protected CommandExecutor commandExecutor;

    protected Map<Class<?>, Session> sessions = new HashMap<>();


    protected Throwable exception;
    protected ClassLoader classLoader;
    protected boolean useClassForNameClassLoading;
    protected boolean reused;


    protected List<CommandContextCloseListener> closeListeners;
    /**
     *
     */
    protected LinkedList<Object> resultStack=new LinkedList<>();

    // General-purpose storing of anything during the lifetime of a command context
    /**
     * //在命令上下文的生命周期内通用存储任何内容
     */
    protected Map<String, Object> attributes = new HashMap<>();




    public CommandContext(Command<?> cmd) {
        this.command = cmd;
    }


    public void close() {

        try {//主要针对finally中的closeSession
            try {//主要针对inner中的 finally 抛出异常
                innerClose();
            } catch (Throwable throwable) {
                exception(throwable);
            } finally {
                closeSessions();
            }
        } catch (Throwable throwable) {
            exception(throwable);
        }
        if (exception != null) {
            rethrowExceptionIfNeeded();
        }


    }

    public void resetException(){
        this.exception = null;

    }

    protected void rethrowExceptionIfNeeded() throws Error{
        if (exception instanceof Error) {
            throw (Error) exception;
        } else if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }else {
            throw new FlowableException();
        }
    }

    protected  void closeSessions(){
        for (Session session : sessions.values()) {
            try {
                session.close();
            } catch (Throwable exception) {
                exception(exception);
            }
        }
    }
    public void innerClose(){
        // The intention of this method is that all resources are closed properly, even if exceptions occur
        // in close or flush methods of the sessions or the transaction context.
        /**
         * //该方法的目的是，即使发生异常，也会正确关闭所有资源
         * //在会话或事务上下文的close或flush方法中
         */

        try {
            /**
             * 首先执行closing方法，如果closing方法出现异常，则会到catch 和finally
             */
            executeCloseListenerClosing();
            if (exception != null) {
                flushSessions();
            }
        } catch (Throwable throwable) {
            exception(throwable);
        } finally {
            try {
                //如果之前的closing 或者flushSession出现异常，则表示close 失败，exception不为空；否则表示 sessionFlush成功
                if (exception == null) {
                    executeCloseListenerAfterSessionFlushed();
                }
            } catch (Throwable throwable) {
                exception(throwable);
            }
            //如果之前的closing 或者flushSession出现异常，或者执行 afterSessionFlushed异常，则表示close 失败，exception不为空；
            if (exception != null) {
                executeCloseListenersCloseFailure();
            } else {
                executeCloseListenerClosed();
            }

        }

    }


    protected void executeCloseListenerClosed() {
        if (closeListeners != null) {
            try {
                for (int i = 0; i < closeListeners.size(); i++) {
                    closeListeners.get(i).closed(this);
                }
            } catch (Throwable throwable) {
                exception(throwable);
            }
        }
    }

    protected void executeCloseListenersCloseFailure() {
        if (closeListeners != null) {
            try {
                for (int i = 0; i < closeListeners.size(); i++) {
                    closeListeners.get(i).closeFailure(this);
                }
            } catch (Throwable throwable) {
                exception(throwable);
            }
        }
    }

    protected void executeCloseListenerAfterSessionFlushed() {
        if (closeListeners != null) {
            try {
                for (int i = 0; i < closeListeners.size(); i++) {
                    closeListeners.get(i).afterSessionFlush(this);

                }

            } catch (Throwable throwable) {
                exception(throwable);
            }

        }
    }

    protected void flushSessions() {
        for (Session session : sessions.values()) {
            session.flush();

        }
    }

    protected void executeCloseListenerClosing() {
        if (closeListeners != null) {
            try {

                for (int i = 0; i < closeListeners.size(); i++) {
                    closeListeners.get(i).closing(this);
                }

            } catch (Exception e) {
                exception(e);
            }
        }

    }

    public void exception(Throwable exception) {
        if (this.exception == null) {
            this.exception = exception;
        } else {
            //log
        }

    }

    public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
        if (closeListeners == null) {
            closeListeners = new ArrayList<>();
        }
        if (!commandContextCloseListener.multipleAllowed()) {
            for (CommandContextCloseListener closeListener : closeListeners) {
                if (closeListener.getClass().equals(commandContextCloseListener.getClass())) {
                    return;
                }
            }
        }
        closeListeners.add(commandContextCloseListener);
        closeListeners.sort(Comparator.comparing(CommandContextCloseListener::order));

    }

    public void setResult(Object result) {
        resultStack.add(result);
    }
    public Object getResult(){
        return resultStack.pollLast();
    }


    public void addAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>(1);
        }
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        if (attributes != null) {
            return attributes.get(key);
        }
        return null;
    }

    public void removeAttribute(String key) {
        if (attributes != null) {
            attributes.remove(key);

            if (attributes.isEmpty()) {
                attributes = null;
            }
        }
    }

}

