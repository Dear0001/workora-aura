package com.workora.work.service;

import com.workora.work.client.OrganizationClient;
import com.workora.work.client.ProjectClient;
import com.workora.work.client.ProjectContext;
import com.workora.work.client.CallerMembership;
import com.workora.work.entity.Task;
import com.workora.work.entity.TaskStatus;
import com.workora.work.exception.ForbiddenException;
import com.workora.work.exception.NotFoundException;
import com.workora.work.repository.TaskAssigneeRepository;
import com.workora.work.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes task access checks and delegates company membership decisions to
 * the Organization service.
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
        try {
            var membership = organizationClient.myMembership(context.companyId());
            return canManageInContext(context, caller, membership);
        } catch (ForbiddenException | NotFoundException ex) {
            return false;
        }
    }

    public CallerMembership requireCompanyMember(UUID companyId) {
        return organizationClient.myMembership(companyId);
    }

    public boolean canManageInContext(
            ProjectContext context, CurrentUser caller, CallerMembership membership) {
        return membership.isOwnerOrAdmin()
                || (membership.isProjectManagerOf(context.departmentId())
                && context.projectManagerId() != null
                && context.projectManagerId().equals(caller.userId()));
    }

    public void requireCanCreateTask(ProjectContext context, CurrentUser caller) {
        if (!canManageInContext(context, caller)) {
            throw new ForbiddenException(
                    "Only the project manager or a company owner/administrator can create tasks in this phase",
                    "TASK_CREATE_DENIED");
        }
    }

    public boolean canManage(Task task, CurrentUser caller) {
        if (task.isArchived()) {
            return false;
        }
        try {
            var membership = organizationClient.myMembership(task.getCompanyId());
            if (membership.isOwnerOrAdmin()) {
                return true;
            }
            if (!membership.isProjectManagerOf(task.getDepartmentId())) {
                return false;
            }
            ProjectContext context = projectClient.resolvePhase(task.getPhaseId());
            return membership.isProjectManagerOf(context.departmentId())
                    && context.projectManagerId() != null
                    && context.projectManagerId().equals(caller.userId());
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

    public void requireCanArchiveTask(Task task) {
        var membership = organizationClient.myMembership(task.getCompanyId());
        if (!membership.isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "Only a company owner or administrator can archive a task",
                    "TASK_ARCHIVE_DENIED");
        }
    }

    public boolean canView(Task task, CurrentUser caller) {
        if (task.isArchived()) {
            return false;
        }
        try {
            var membership = organizationClient.myMembership(task.getCompanyId());
            return canView(task, caller, membership);
        } catch (ForbiddenException | NotFoundException ex) {
            return false;
        }
    }

    public boolean canView(
            Task task, CurrentUser caller, CallerMembership membership) {
        if (membership.isOwnerOrAdmin()) {
            return true;
        }
        if (membership.isProjectManagerOf(task.getDepartmentId())) {
            ProjectContext context = projectClient.resolvePhase(task.getPhaseId());
            if (canManageInContext(context, caller, membership)) {
                return true;
            }
        }
        return taskAssigneeRepository.existsByTaskIdAndUserIdAndRemovedAtIsNull(
                task.getId(), caller.userId());
    }

    public void requireCanViewTask(Task task, CurrentUser caller) {
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

    public void requireEligibleAssignee(Task task, UUID assigneeId) {
        if (task.getCreatedBy().equals(assigneeId)) {
            throw new ForbiddenException(
                    "You cannot assign a task you created to yourself. Please select another eligible member.",
                    "TASK_CREATOR_SELF_ASSIGNMENT_DENIED");
        }
        var candidate = organizationClient.member(task.getCompanyId(), assigneeId);
        if (!candidate.isDeveloperOrTesterIn(task.getDepartmentId())) {
            throw new ForbiddenException(
                    "Task assignees must be an active Developer or Tester in the task department",
                    "TASK_ASSIGNEE_NOT_ELIGIBLE");
        }
    }

    public void requireCanChangeStatus(Task task, CurrentUser caller, TaskStatus target) {
        if (canManage(task, caller)) {
            return;
        }
        requireActiveAssignee(task, caller);
        String role = organizationClient.member(task.getCompanyId(), caller.userId())
                .roleIn(task.getDepartmentId());
        boolean developerTransition = "DEVELOPER".equals(role)
                && (target == TaskStatus.IN_PROGRESS || target == TaskStatus.SUBMITTED);
        boolean testerTransition = "TESTER".equals(role)
                && task.getStatus() == TaskStatus.SUBMITTED
                && (target == TaskStatus.ACCEPTED || target == TaskStatus.CHANGES_REQUESTED);
        if (!developerTransition && !testerTransition) {
            throw new ForbiddenException(
                    "Your company role cannot make this task status change",
                    "TASK_STATUS_CHANGE_DENIED");
        }
    }
}
