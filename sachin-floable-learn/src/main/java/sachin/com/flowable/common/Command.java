package sachin.com.flowable.common;

import sachin.com.flowable.common.cfg.CommandContext;

public interface Command<T>{

    T execute(CommandContext commandContext);

}

