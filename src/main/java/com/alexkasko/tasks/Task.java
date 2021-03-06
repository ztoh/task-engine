package com.alexkasko.tasks;

/**
 * Interface for asynchronous multistage tasks.
 * It's implied, that task has two different fields to represent current state: "stage" and "status".
 * Stage (e.g. 'loading_data', 'finished') is and task processing state,
 * Status (e.g. 'error', 'processing') is a task application status
 *
 * @author alexkasko
 * Date: 5/17/12
 * @see TaskEngine
 * @see TaskStageChain
 * @see TaskManager
 */
public interface Task {
    /**
     * Should be implemented as static method on the upper level of hierarchy
     *
     * @return all stages this task may be into during processing
     */
    TaskStageChain stageChain();

    /**
     * Returns task instance unique id
     *
     * @return task instance unique id
     */
    long getId();

    /**
     * Current stage for this task
     *
     * @return current stage for this task
     */
    String getStageName();
}
