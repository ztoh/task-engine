package com.alexkasko.tasks.impl.hibernate;

import com.alexkasko.tasks.impl.Stage;
import com.alexkasko.tasks.impl.Status;
import com.alexkasko.tasks.impl.TaskImpl;
import com.alexkasko.tasks.impl.TaskManagerIface;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.type.LongType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

/**
 * User: alexkasko
 * Date: 5/23/12
 */

// query literals are deliberate
@Service
public class TaskManagerHibernateImpl implements TaskManagerIface {
    @Inject
    private SessionFactory sf;
    private Session cs() {return sf.getCurrentSession();}

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Collection<TaskImpl> markProcessingAndLoad() {
        // lock selected
        int updated = cs().createSQLQuery("update tasks set status='SELECTED' where (status='NORMAL' and stage='CREATED') or status='RESUMED'")
                .executeUpdate();
        if(0 == updated) return ImmutableList.of();
        // load selected
        List<TaskImpl> taskList = cs().createQuery("from TaskImpl where status='SELECTED'").list();
        List<Long> taskIds = Lists.transform(taskList, TaskImpl.ID_FUNCTION);
        // mark
        cs().createSQLQuery("update tasks set status='PROCESSING' where id in (:taskIds)")
                .setParameterList("taskIds", taskIds)
                .executeUpdate();
        // returned status is not synchronized with db, but that doesn't matter
        return taskList;
    }

    @Override
    @Transactional
    public void updateStage(long taskId, String stage) {
        Stage st = Stage.valueOf(stage);
        TaskImpl task = (TaskImpl) cs().get(TaskImpl.class, taskId);
        if(!Status.PROCESSING.equals(task.getStatus())) throw new IllegalStateException(
                "updateStage method must be called only on 'processing tasks'");
        task.changeStage(st);
        cs().update(task);
    }

    @Override
    @Transactional
    public void updateStatusSuccess(long taskId) {
        TaskImpl task = (TaskImpl) cs().get(TaskImpl.class, taskId);
        if(!Status.PROCESSING.equals(task.getStatus())) throw new IllegalStateException(
                        "updateStage method must be called only on 'processing tasks'");
        task.changeStatus(Status.NORMAL);
        cs().update(task);
    }

    @Override
    @Transactional
    public void updateStatusSuspended(long taskId) {
        TaskImpl task = (TaskImpl) cs().get(TaskImpl.class, taskId);
        if(!Status.PROCESSING.equals(task.getStatus())) throw new IllegalStateException(
                        "updateStage method must be called only on 'processing tasks'");
        task.changeStatus(Status.SUSPENDED);
        cs().update(task);
    }

    @Override
    @Transactional
    public void updateStatusError(long taskId, Exception e, String lastCompletedStage) {
        e.printStackTrace();
        Stage stage = Stage.valueOf(lastCompletedStage);
        TaskImpl task = (TaskImpl) cs().get(TaskImpl.class, taskId);
        if(!Status.PROCESSING.equals(task.getStatus())) throw new IllegalStateException(
                        "updateStage method must be called only on 'processing tasks'");
        task.changeStatus(Status.ERROR);
        task.changeStage(stage);
    }

    @Override
    @Transactional
    public long add(TaskImpl task) {
        return (Long) cs().save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskImpl load(long taskId) {
        return (TaskImpl) cs().get(TaskImpl.class, taskId);
    }

    @Override
    @Transactional
    public void resume(long taskId) {
        TaskImpl task = (TaskImpl) cs().get(TaskImpl.class, taskId);
        task.changeStatus(Status.RESUMED);
        cs().update(task);
    }
}
