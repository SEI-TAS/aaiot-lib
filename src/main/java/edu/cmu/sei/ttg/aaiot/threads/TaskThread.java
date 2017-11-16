package edu.cmu.sei.ttg.aaiot.threads;

import javafx.concurrent.Task;

/**
 * Created by sebastianecheverria on 11/16/17.
 */
public class TaskThread
{
    // Simple interface for the execution object.
    public interface ITaskExecution
    {
        void execute();
    }

    // Contains the method that is actually executed in the thread.
    private ITaskExecution taskExecution;

    /**
     * Constructor.
     * @param taskExecution
     */
    public TaskThread(ITaskExecution taskExecution)
    {
        this.taskExecution = taskExecution;
    }

    /**
     * Sets up the task with the actual execution method.
     */
    public void start()
    {
        Task<Void> task = new Task<Void>()
        {
            @Override
            protected Void call() throws Exception
            {
                taskExecution.execute();
                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.start();
    }
}
