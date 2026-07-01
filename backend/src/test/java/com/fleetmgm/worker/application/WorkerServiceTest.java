package com.fleetmgm.worker.application;

import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.worker.domain.Worker;
import com.fleetmgm.worker.domain.WorkerRole;
import com.fleetmgm.worker.dto.CreateWorkerRequest;
import com.fleetmgm.worker.dto.WorkerMapper;
import com.fleetmgm.worker.dto.WorkerResponse;
import com.fleetmgm.worker.infrastructure.WorkerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

    @Mock WorkerRepository workerRepository;
    @Mock UserRepository userRepository;
    @Mock WorkerMapper workerMapper;
    @InjectMocks WorkerService workerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsAndReturnsDto_whenValid() {
        CreateWorkerRequest request = new CreateWorkerRequest(
                "Juan", "García", WorkerRole.DRIVER, "12345678A", null, null, null, null);
        Worker entity = new Worker();
        WorkerResponse expected = buildWorkerResponse(UUID.randomUUID());

        when(workerRepository.existsByNationalId("12345678A")).thenReturn(false);
        when(workerMapper.toEntity(request)).thenReturn(entity);
        when(workerRepository.save(entity)).thenReturn(entity);
        when(workerMapper.toResponse(entity)).thenReturn(expected);

        WorkerResponse result = workerService.create(request);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<Worker> captor = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
    }

    @Test
    void create_withUserId_linksUser_whenUserExists() {
        UUID userId = UUID.randomUUID();
        CreateWorkerRequest request = new CreateWorkerRequest(
                "Juan", "García", WorkerRole.DRIVER, "12345678A", null, null, null, userId);
        Worker entity = new Worker();
        User mockUser = new User();
        WorkerResponse expected = buildWorkerResponse(UUID.randomUUID());

        when(workerRepository.existsByNationalId("12345678A")).thenReturn(false);
        when(workerMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(workerRepository.save(entity)).thenReturn(entity);
        when(workerMapper.toResponse(entity)).thenReturn(expected);

        workerService.create(request);

        ArgumentCaptor<Worker> captor = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(mockUser);
    }

    @Test
    void create_throwsConflict_whenNationalIdDuplicated() {
        CreateWorkerRequest request = new CreateWorkerRequest(
                "Juan", "García", WorkerRole.DRIVER, "12345678A", null, null, null, null);
        when(workerRepository.existsByNationalId("12345678A")).thenReturn(true);

        assertThatThrownBy(() -> workerService.create(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("WORKER_NATIONAL_ID_CONFLICT"));

        verify(workerRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_returnsDto_whenFound() {
        UUID id = UUID.randomUUID();
        Worker entity = new Worker();
        WorkerResponse expected = buildWorkerResponse(id);

        when(workerRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workerMapper.toResponse(entity)).thenReturn(expected);

        assertThat(workerService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(workerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workerService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode())
                        .isEqualTo("WORKER_NOT_FOUND"));
    }

    // --- delete ---

    @Test
    void delete_softDeletes_whenExists() {
        UUID id = UUID.randomUUID();
        Worker entity = new Worker();

        when(workerRepository.findById(id)).thenReturn(Optional.of(entity));
        when(workerRepository.save(entity)).thenReturn(entity);

        workerService.delete(id);

        ArgumentCaptor<Worker> captor = ArgumentCaptor.forClass(Worker.class);
        verify(workerRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    // --- list as DRIVER ---

    @Test
    void list_asDriver_returnsOnlyOwnProfile() {
        var auth = new UsernamePasswordAuthenticationToken(
                "driver@example.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID userId = UUID.randomUUID();
        User mockUser = new User();
        // We need to be able to call user.getId() — User has no setter for id,
        // so we use a real-ish User with reflection or just mock it.
        // Since User is not final, we can mock it directly.
        User mockedUser = mock(User.class);
        when(mockedUser.getId()).thenReturn(userId);

        Worker worker = new Worker();
        WorkerResponse workerResponse = buildWorkerResponse(UUID.randomUUID());

        when(userRepository.findByEmail("driver@example.com")).thenReturn(Optional.of(mockedUser));
        when(workerRepository.findByUserId(userId)).thenReturn(Optional.of(worker));
        when(workerMapper.toResponse(worker)).thenReturn(workerResponse);

        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<WorkerResponse> result = workerService.list(pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0)).isEqualTo(workerResponse);
        assertThat(result.totalElements()).isEqualTo(1L);
    }

    // --- helpers ---

    private WorkerResponse buildWorkerResponse(UUID id) {
        return new WorkerResponse(id, "Juan", "García", "Juan García",
                WorkerRole.DRIVER, "12345678A", null, null, null, null, Instant.now());
    }
}
