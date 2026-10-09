package com.workora.organization.scheduler;

import com.workora.organization.service.InvitationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InvitationLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvitationLifecycleScheduler.class);

    private final InvitationService invitationService;

    public InvitationLifecycleScheduler(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Scheduled(fixedDelayString = "${organization.invitations.expiration-check-ms:60000}")
    public void expireInvitations() {
        int expired = invitationService.expireExpiredInvitations();
        if (expired > 0) {
            log.info("Expired {} organization invitations", expired);
        }
    }

    @Scheduled(fixedDelayString = "${organization.invitations.reminder-check-ms:3600000}")
    public void sendReminderNotifications() {
        int reminded = invitationService.remindPendingInvitations();
        if (reminded > 0) {
            log.info("Queued reminders for {} organization invitations", reminded);
        }
    }
}
