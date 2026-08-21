package com.oddlabs.tt.base.resource;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FileTest {

    private static final class TestFile extends File<String> {
        TestFile(URI uri) {
            super(uri);
        }

        @Override
        public String get() {
            return "test-content";
        }
    }

    @Test
    void testFilePropertiesAndEquality() {
        URI uri1 = URI.create("file:///test/path1.txt");
        URI uri2 = URI.create("file:///test/path1.txt");
        URI uri3 = URI.create("file:///test/path2.txt");

        TestFile file1 = new TestFile(uri1);
        TestFile file2 = new TestFile(uri2);
        TestFile file3 = new TestFile(uri3);

        assertEquals(file1, file2);
        assertEquals(file1.hashCode(), file2.hashCode());
        assertNotEquals(file1, file3);
        assertNotNull(file1.getURL());
        assertEquals("test-content", file1.get());
    }
}
