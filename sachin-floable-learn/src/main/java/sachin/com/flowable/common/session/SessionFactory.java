package sachin.com.flowable.common.session;

import sachin.com.flowable.common.cfg.CommandContext;
import sachin.com.flowable.common.cfg.CommandContextFactory;

public interface   SessionFactory {

    Class<?> getSessionType();

    Session openSession(CommandContext commandContext);

}
