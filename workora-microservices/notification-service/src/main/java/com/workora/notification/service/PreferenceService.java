package com.workora.notification.service;

import com.workora.notification.dto.PreferenceResponse;
import com.workora.notification.dto.PreferenceUpsertRequest;
import com.workora.notification.entity.NotificationChannel;
import com.workora.notification.entity.NotificationPreference;
import com.workora.notification.entity.NotificationType;
import com.workora.notification.mapper.NotificationMapper;
import com.workora.notification.repository.NotificationPreferenceRepository;
import com.workora.notification.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class PreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMapper mapper;

    public PreferenceService(NotificationPreferenceRepository preferenceRepository, NotificationMapper mapper) {
        this.preferenceRepository = preferenceRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PreferenceResponse> list(CurrentUser caller) {
        List<NotificationPreference> stored = preferenceRepository.findByUserId(caller.userId());
        if (!stored.isEmpty()) {
            return stored.stream().map(mapper::toResponse).toList();
        }
        // Defaults are generated on the fly (never persisted): everything enabled in-app.
        return Arrays.stream(NotificationType.values())
                .map(type -> new PreferenceResponse(
                        null, caller.userId(), NotificationChannel.IN_APP, type, true, null))
                .toList();
    }

    public PreferenceResponse upsert(CurrentUser caller, PreferenceUpsertRequest req) {
        NotificationPreference preference = preferenceRepository
                .findByUserIdAndChannelAndType(caller.userId(), req.channel(), req.type())
                .orElseGet(() -> new NotificationPreference(
                        caller.userId(), req.channel(), req.type(), req.enabled()));
        preference.setEnabled(req.enabled());
        return mapper.toResponse(preferenceRepository.save(preference));
    }
}
