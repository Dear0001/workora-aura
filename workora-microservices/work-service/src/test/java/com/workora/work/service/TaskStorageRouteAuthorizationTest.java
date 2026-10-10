package com.workora.work.service;

import com.workora.common.storage.StorageObjectController;
import com.workora.common.storage.StorageObjectService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TaskStorageRouteAuthorizationTest {

    private final StorageObjectService storage = mock(StorageObjectService.class);
    private final StorageObjectController controller = new StorageObjectController(storage);

    @Test
    void genericStorageRoutesRejectTaskNamespace() {
        assertThatThrownBy(() -> controller.createUpload(
                null, new StorageObjectController.UploadRequest("tasks", "private.txt", "text/plain")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.createDownload(null, "tasks/private-key.txt"))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> controller.delete(null, "tasks/private-key.txt"))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(storage);
    }
}
