package sachin.com.flowable.common.cfg;

import sachin.com.flowable.common.Command;
import sachin.com.flowable.common.session.SessionFactory;

import java.util.Map;

public class CommandContextFactory {

    protected Map<Class<?>, SessionFactory> sessionFactories;


    public CommandContext createCommandContext(Command<?>cmd){
        CommandContext commandContext = new CommandContext(cmd);
        //commandContext.setSessionFactories(sessionFactories);
        return commandContext;
    }


    public Map<Class<?>, SessionFactory> getSessionFactories() {
        return sessionFactories;
    }

    public void setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
        this.sessionFactories = sessionFactories;
    }
}
