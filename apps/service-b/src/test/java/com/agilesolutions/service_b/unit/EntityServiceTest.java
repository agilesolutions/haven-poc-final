package com.agilesolutions.service_b.unit;

import com.agilesolutions.service_b.model.Entity;
import com.agilesolutions.service_b.repository.EntityRepository;
import com.agilesolutions.service_b.service.EntityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EntityService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EntityService Unit Tests")
class EntityServiceTest {

    @Mock
    private EntityRepository entityRepository;

    @InjectMocks
    private EntityService entityService;

    private Entity testEntity;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testEntity = Entity.builder()
                .id(testId)
                .name("Test Entity")
                .description("A test entity for unit testing")
                .version("1.0.0")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy("test-user")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should find entity by ID successfully")
    void testFindById_Success() {
        // Given
        when(entityRepository.findById(testId)).thenReturn(Optional.of(testEntity));

        // When
        Entity result = entityService.findById(testId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getName()).isEqualTo("Test Entity");
        verify(entityRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should throw 404 when entity not found by ID")
    void testFindById_NotFound() {
        // Given
        when(entityRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> entityService.findById(testId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Entity not found");
        verify(entityRepository, times(1)).findById(testId);
    }

    @Test
    @DisplayName("Should find active entity by ID successfully")
    void testFindActiveById_Success() {
        // Given
        when(entityRepository.findByIdAndActive(testId, true))
                .thenReturn(Optional.of(testEntity));

        // When
        Entity result = entityService.findActiveById(testId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getActive()).isTrue();
        verify(entityRepository, times(1)).findByIdAndActive(testId, true);
    }

    @Test
    @DisplayName("Should find entity by name successfully")
    void testFindByName_Success() {
        // Given
        when(entityRepository.findActiveByName("Test Entity")).thenReturn(Optional.of(testEntity));

        // When
        Entity result = entityService.findByName("Test Entity");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Entity");
        verify(entityRepository, times(1)).findActiveByName("Test Entity");
    }

    @Test
    @DisplayName("Should throw 404 when entity not found by name")
    void testFindByName_NotFound() {
        // Given
        when(entityRepository.findActiveByName(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> entityService.findByName("NonExistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Entity not found");
    }

    @Test
    @DisplayName("Should get all active entities")
    void testGetAllActive() {
        // Given
        Entity entity2 = Entity.builder()
                .id(UUID.randomUUID())
                .name("Entity 2")
                .version("1.0.0")
                .active(true)
                .build();
        List<Entity> entities = List.of(testEntity, entity2);
        when(entityRepository.findAllActive()).thenReturn(entities);

        // When
        List<Entity> result = entityService.getAllActive();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testEntity, entity2);
        verify(entityRepository, times(1)).findAllActive();
    }

    @Test
    @DisplayName("Should create entity successfully")
    void testCreate_Success() {
        // Given
        Entity newEntity = Entity.builder()
                .name("New Entity")
                .description("New test entity")
                .version("1.0.0")
                .build();
        when(entityRepository.save(any(Entity.class))).thenReturn(testEntity);

        // When
        Entity result = entityService.create(newEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testId);
        assertThat(result.getActive()).isTrue();
        verify(entityRepository, times(1)).save(any(Entity.class));
    }

    @Test
    @DisplayName("Should set default version when creating entity without version")
    void testCreate_WithDefaultVersion() {
        // Given
        Entity newEntity = Entity.builder()
                .name("Entity Without Version")
                .build();
        Entity savedEntity = Entity.builder()
                .id(testId)
                .name("Entity Without Version")
                .version("1.0.0")
                .active(true)
                .build();
        when(entityRepository.save(any(Entity.class))).thenReturn(savedEntity);

        // When
        Entity result = entityService.create(newEntity);

        // Then
        assertThat(result.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("Should update entity successfully")
    void testUpdate_Success() {
        // Given
        when(entityRepository.findById(testId)).thenReturn(Optional.of(testEntity));
        Entity updateData = Entity.builder()
                .name("Updated Name")
                .description("Updated description")
                .version("2.0.0")
                .build();
        when(entityRepository.save(any(Entity.class))).thenReturn(testEntity);

        // When
        Entity result = entityService.update(testId, updateData);

        // Then
        assertThat(result).isNotNull();
        verify(entityRepository, times(1)).findById(testId);
        verify(entityRepository, times(1)).save(any(Entity.class));
    }

    @Test
    @DisplayName("Should check if active entity exists")
    void testExistsActive() {
        // Given
        when(entityRepository.existsActiveById(testId)).thenReturn(true);

        // When
        boolean exists = entityService.existsActive(testId);

        // Then
        assertThat(exists).isTrue();
        verify(entityRepository, times(1)).existsActiveById(testId);
    }

    @Test
    @DisplayName("Should delete (deactivate) entity successfully")
    void testDelete_Success() {
        // Given
        when(entityRepository.findById(testId)).thenReturn(Optional.of(testEntity));
        when(entityRepository.save(any(Entity.class))).thenReturn(testEntity);

        // When
        entityService.delete(testId);

        // Then
        verify(entityRepository, times(1)).findById(testId);
        verify(entityRepository, times(1)).save(any(Entity.class));
    }
}

