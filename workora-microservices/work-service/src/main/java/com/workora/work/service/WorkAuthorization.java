package com.workora.work.service;

import com.workora.work.client.OrganizationClient;
import com.workora.work.client.ProjectClient;
import com.workora.work.client.ProjectContext;
import com.workora.work.entity.Task;
import com.workora.work.exception.ForbiddenException;
import com.workora.work.exception.NotFoundException;
import com.workora.work.repository.TaskAssigneeRepository;
import com.workora.work.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes resource-level authorization for the Work domain. Tasks are private
 * work items: only the project manager, company owner/admin, or an active assignee
 * may see or act on them.
 */
@Component
public class WorkAuthorization {

    private final OrganizationClient organizationClient;
    private final ProjectClient projectClient;
    private final TaskAssigneeRepository taskAssigneeRepository;

    public WorkAuthorization(OrganizationClient organizationClient,
                             ProjectClient projectClient,
                             TaskAssigneeRepository taskAssigneeRepository) {
        this.organizationClient = organizationClient;
        this.projectClient = projectClient;
        this.taskAssigneeRepository = taskAssigneeRepository;
    }

    public boolean canManageInContext(ProjectContext context, CurrentUser caller) {
        if (context.projectManagerId() != null && context.projectManagerId().equals(caller.userId())) {
            return true;
        }
        try {
            return organizationClient.myMembership(context.companyId()).isOwnerOrAdmin();
        } catch (ForbiddenException | NotFoundException ex) {
            return false;
        }
    }

    public void requireCanCreateTask(ProjectContext context, CurrentUser caller) {
        if (!canManageInContext(context, caller)) {
            throw new ForbiddenException(
                    "Only the project manager or a company owner/administrator can create tasks in this phase",
                    "TASK_CREATE_DENIED");
        }
    }

    public boolean canManage(Task task, CurrentUser caller) {
        UUID managerId = task.getProjectManagerId();
        if (managerId == null) {
            try {
                managerId = projectClient.resolvePhase(task.getPhaseId()).projectManagerId();
            } catch (ForbiddenException | NotFoundException ex) {
                managerId = null;
            }
        }
        if (managerId != null && managerId.equals(caller.userId())) {
            return true;
        }
        try {
            return organizationClient.myMembership(task.getCompanyId()).isOwnerOrAdmin();
        } catch (ForbiddenException | NotFoundException ex) {
            return false;
        }
    }

    public void requireCanManageTask(Task task, CurrentUser caller) {
        if (!canManage(task, caller)) {
            throw new ForbiddenException(
                    "Only the project manager or a company owner/administrator can manage this task",
                    "TASK_MANAGE_DENIED");
        }
    }

    public boolean canView(Task task, CurrentUser caller) {
        return canManage(task, caller)
                || taskAssigneeRepository.existsByTaskIdAndUserIdAndRemovedAtIsNull(task.getId(), caller.userId());
    }

    public void requireCanViewTask(Task task, CurrentUser caller) {
        // Tasks are private: an unauthorized caller gets 404 rather than 403 so
        // the existence of the task is not leaked.
        if (!canView(task, caller)) {
            throw new NotFoundException("Task not found");
        }
    }

    public void requireActiveAssignee(Task task, CurrentUser caller) {
        if (!taskAssigneeRepository.existsByTaskIdAndUserIdAndRemovedAtIsNull(task.getId(), caller.userId())) {
            throw new ForbiddenException(
                    "Only an active assignee of this task can perform this action",
                    "NOT_TASK_ASSIGNEE");
        }
    }
}
