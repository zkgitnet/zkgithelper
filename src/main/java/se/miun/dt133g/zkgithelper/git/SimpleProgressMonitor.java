package se.miun.dt133g.zkgithelper.git;

import se.miun.dt133g.zkgithelper.support.IoUtils;

import org.eclipse.jgit.lib.ProgressMonitor;

/**
 * A simple implementation of the ProgressMonitor interface to track the progress of git operations.
 * This class provides basic logging functionality to trace the progress of tasks.
 * @author Leif Rogell
 */
public final class SimpleProgressMonitor implements ProgressMonitor {

    /**
     * Starts the progress monitoring and logs the initiation of the task.
     * @param totalTasks the total number of tasks to track
     */
    @Override
    public void start(final int totalTasks) {
        IoUtils.INSTANCE.trace("Starting fetch...");
    }

    /**
     * Begins a task and logs the title and total work units.
     * @param title the title of the task
     * @param totalWork the total amount of work for this task
     */
    @Override
    public void beginTask(final String title, final int totalWork) {
        IoUtils.INSTANCE.trace(title + " (" + totalWork + " units)");
    }

    /**
     * Updates the progress of the task. This implementation does not track progress.
     * @param completed the number of completed units
     */
    @Override
    public void update(final int completed) { }

    /**
     * Ends the current task. This implementation does not log any specific actions for ending the task.
     */
    @Override
    public void endTask() { }

    /**
     * Checks whether the task has been canceled. This implementation always returns false.
     * @return false indicating the task is not canceled
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Shows or hides the duration of the task based on the input flag.
     * @param b if true, the duration is logged; otherwise, it is not
     */
    @Override
    public void showDuration(final boolean b) {
        if (b) {
            IoUtils.INSTANCE.trace("Showing duration...");
        }
    }
}
