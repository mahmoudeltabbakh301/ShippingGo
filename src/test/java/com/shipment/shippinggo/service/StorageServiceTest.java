package com.shipment.shippinggo.service;

import com.shipment.shippinggo.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(tempDir.toString());
    }

    @Test
    void store_ValidFile_ReturnsFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        String filename = storageService.store(file);

        assertNotNull(filename);
        assertTrue(filename.endsWith(".txt"));
        assertTrue(storageService.load(filename).toFile().exists());
    }

    @Test
    void store_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        assertThrows(StorageException.class, () -> storageService.store(file));
    }

    @Test
    void load_ReturnsPath() {
        Path result = storageService.load("somefile.txt");
        assertNotNull(result);
        assertEquals("somefile.txt", result.getFileName().toString());
    }

    @Test
    void loadAsResource_ExistingFile_ReturnsResource() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resource.txt", "text/plain", "content".getBytes());
        String storedName = storageService.store(file);

        Resource resource = storageService.loadAsResource(storedName);

        assertNotNull(resource);
        assertTrue(resource.exists());
    }

    @Test
    void loadAsResource_NonExistingFile_ThrowsException() {
        assertThrows(StorageException.class,
                () -> storageService.loadAsResource("nonexistent.txt"));
    }

    @Test
    void store_FileWithoutExtension_Succeeds() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "noext", "application/octet-stream", "data".getBytes());

        String filename = storageService.store(file);
        assertNotNull(filename);
    }
}
