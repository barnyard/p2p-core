package com.bt.pi.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ContextDiscovererTest {
    private ContextDiscoverer contextDiscoverer;
    private File testClassPathDirectory1;
    private File testClassPathDirectory2;

    @Before
    public void before() throws Exception {
        testClassPathDirectory1 = File.createTempFile("unittesting", "");
        FileUtils.deleteQuietly(testClassPathDirectory1);
        FileUtils.forceMkdir(testClassPathDirectory1);
        testClassPathDirectory2 = File.createTempFile("unittesting", "");
        FileUtils.deleteQuietly(testClassPathDirectory2);
        FileUtils.forceMkdir(testClassPathDirectory2);
    }

    @After
    public void after() {
        FileUtils.deleteQuietly(testClassPathDirectory1);
        FileUtils.deleteQuietly(testClassPathDirectory2);
    }

    @Test
    public void testGetClasspath() {
        // setup
        this.contextDiscoverer = new ContextDiscoverer();

        // act
        String result = contextDiscoverer.getClasspath();

        // assert
        assertEquals(System.getProperty("java.class.path"), result);
    }

    @Test
    public void shouldFindFileInDir() throws Exception {
        // setup
        String filename = "applicationContext-p2p-test.xml";
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory1.getAbsolutePath(), filename)));
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath();
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        assertEquals(filename, result);
    }

    @Test
    public void shouldFindTwoFilesInDir() throws Exception {
        // setup
        String filename1 = "applicationContext-p2p-test1.xml";
        String filename2 = "applicationContext-p2p-test2.xml";
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory1.getAbsolutePath(), filename1)));
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory1.getAbsolutePath(), filename2)));
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath();
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        List<String> files = Arrays.asList(result.split(","));
        assertEquals(2, files.size());
        assertTrue(files.contains(filename1));
        assertTrue(files.contains(filename2));
    }

    @Test
    public void shouldFindTwoFilesInDifferentDirs() throws Exception {
        // setup
        String filename1 = "applicationContext-p2p-test1.xml";
        String filename2 = "applicationContext-p2p-test2.xml";
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory1.getAbsolutePath(), filename1)));
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory2.getAbsolutePath(), filename2)));
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath() + System.getProperty("path.separator") + testClassPathDirectory2.getAbsolutePath();
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        List<String> files = Arrays.asList(result.split(","));
        assertEquals(2, files.size());
        assertTrue(files.contains(filename1));
        assertTrue(files.contains(filename2));
    }

    @Test
    public void shouldIgnoreIntegrationFiles() throws Exception {
        // setup
        String filename1 = "applicationContext-p2p-test1.xml";
        String filename2 = "applicationContext-p2p-integration.xml";
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory1.getAbsolutePath(), filename1)));
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory2.getAbsolutePath(), filename2)));
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath() + System.getProperty("path.separator") + testClassPathDirectory2.getAbsolutePath();
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        List<String> files = Arrays.asList(result.split(","));
        assertEquals(1, files.size());
        assertTrue(files.contains(filename1));
    }

    @Test
    public void shouldIgnoreNonP2pFiles() throws Exception {
        // setup
        String filename1 = "applicationContext-p2p-test1.xml";
        String filename2 = "applicationContext-other.xml";
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory1.getAbsolutePath(), filename1)));
        FileUtils.touch(new File(String.format("%s/%s", testClassPathDirectory2.getAbsolutePath(), filename2)));
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath() + System.getProperty("path.separator") + testClassPathDirectory2.getAbsolutePath();
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        List<String> files = Arrays.asList(result.split(","));
        assertEquals(1, files.size());
        assertTrue(files.contains(filename1));
    }

    @Test
    public void shouldFindFilesInJar() throws Exception {
        // setup
        String filename1 = "applicationContext-p2p-test1.xml";
        final String jarFilename1 = "test1.jar";

        createJar(testClassPathDirectory1, jarFilename1, filename1);
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath() + System.getProperty("file.separator") + jarFilename1;
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        List<String> files = Arrays.asList(result.split(","));
        assertEquals(1, files.size());
        assertTrue(files.contains(filename1));
    }

    @Test
    public void shouldFindFilesInJarButOnlyInRoot() throws Exception {
        // setup
        String filename1 = "applicationContext-p2p-test1.xml";
        String filename2 = "test/applicationContext-p2p-test2.xml";
        String filename3 = "test1/";
        final String jarFilename1 = "test1.jar";

        createJar(testClassPathDirectory1, jarFilename1, filename1, filename2, filename3);
        this.contextDiscoverer = new ContextDiscoverer() {
            @Override
            protected String getClasspath() {
                return testClassPathDirectory1.getAbsolutePath() + System.getProperty("file.separator") + jarFilename1;
            }
        };

        // act
        String result = this.contextDiscoverer.findPiContexts();

        // assert
        List<String> files = Arrays.asList(result.split(","));
        assertEquals(1, files.size());
        assertTrue(files.contains(filename1));
    }

    private void createJar(File dir, String jarFilename, String... filenames) throws Exception {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

        JarOutputStream jos = new JarOutputStream(byteOut);
        for (String filename : filenames) {
            jos.putNextEntry(new JarEntry(filename));
            jos.write("abc".getBytes());
            jos.closeEntry();
        }
        jos.close();

        String outFile = String.format("%s/%s", dir.getAbsolutePath(), jarFilename);
        IOUtils.write(byteOut.toByteArray(), new FileOutputStream(outFile));
    }
}
