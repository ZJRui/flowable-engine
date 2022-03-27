package sachin.com.flowable.common.service;

import sachin.com.flowable.common.executor.CommandExecutor;
import lombok.Data;

@Data
public class JobServiceConfiguration {


    protected CommandExecutor commandExecutor;

}
