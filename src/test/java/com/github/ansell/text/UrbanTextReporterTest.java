/**
 * 
 */
package com.github.ansell.text;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import joptsimple.OptionException;

/**
 * Tests for {@link UrbanTextReporter}
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class UrbanTextReporterTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Rule
	public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

	private Path testDir;

	private Path testFile;

	@Before
	public void setUp() throws Exception {
		testDir = tempDir.newFolder().toPath();
		testFile = testDir.resolve("test.dat");
		Files.write(testFile, "".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void testMainUnknownOption() throws Exception {
		thrown.expect(OptionException.class);
		UrbanTextReporter.main("--unknown-option");
	}

	@Test
	public final void testMainMissingInput() throws Exception {
		thrown.expect(IOException.class);
		UrbanTextReporter.main("--input", testDir.resolve("does-not-exist.dat").toAbsolutePath().toString());
	}

	/**
	 * Test method for
	 * {@link com.github.ansell.text.UrbanTextReporter#main(java.lang.String[])}.
	 */
	@Test
	public final void testMainNoArgsUTF8ASCIIRange() throws Exception {
		InputStream previousIn = System.in;
		try {
			// Hack System.in to be our test stream and verify it doesn't fall
			// over it stop working for any reason
			System.setIn(new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8)));
			UrbanTextReporter.main();
		} finally {
			System.setIn(previousIn);
		}
	}

	/**
	 * Test method for
	 * {@link com.github.ansell.text.UrbanTextReporter#main(java.lang.String[])}.
	 */
	@Test
	public final void testMainSingleEmptyStringUTF8ASCIIRange() throws Exception {
		InputStream previousIn = System.in;
		try {
			// Hack System.in to be our test stream and verify it doesn't fall
			// over it stop working for any reason
			System.setIn(new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8)));
			UrbanTextReporter.main("");
		} finally {
			System.setIn(previousIn);
		}
	}

	/**
	 * Test method for
	 * {@link com.github.ansell.text.UrbanTextReporter#main(java.lang.String[])}.
	 */
	@Test
	public final void testMainHelp() throws Exception {
		UrbanTextReporter.main("--help");
	}

	/**
	 * Test method for
	 * {@link com.github.ansell.text.UrbanTextReporter#runReporter(Path, Set, java.io.Writer)}.
	 */
	@Test
	public final void testRunReporterEmptyCharsetSet() throws Exception {
		ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
		PrintStream previousOut = System.out;
		try {
			System.setOut(new PrintStream(testOutput));
			UrbanTextReporter.runReporter(testFile, Collections.emptySet(), new PrintWriter(System.out));

			// If there are no charsets available, the header line should still
			// be printed successfully with no errors
			assertTrue("Unexpected output for empty charsets",
					Arrays.equals("Charset,EncoderError\n".getBytes(StandardCharsets.UTF_8), testOutput.toByteArray()));
		} finally {
			System.setOut(previousOut);
		}
	}

	/**
	 * Test method for
	 * {@link com.github.ansell.text.UrbanTextReporter#runReporter(Path, Set, java.io.Writer)}.
	 */
	@Test
	public final void testRunReporterUTF8() throws Exception {
		ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
		PrintStream previousOut = System.out;
		try {
			System.setOut(new PrintStream(testOutput));
			UrbanTextReporter.runReporter(testFile, Collections.singleton(StandardCharsets.UTF_8),
					new PrintWriter(System.out));

			// If there are no charsets available, the header line should still
			// be printed successfully with no errors
			assertTrue("Unexpected output for UTF-8 only", Arrays.equals(
					"Charset,EncoderError\nUTF-8,\n".getBytes(StandardCharsets.UTF_8), testOutput.toByteArray()));
		} finally {
			System.setOut(previousOut);
		}
	}

	/**
	 * Test method for
	 * {@link com.github.ansell.text.UrbanTextReporter#getPrioritisedCharsets()}.
	 */
	@Test
	public final void testGetPrioritisedCharsets() throws Exception {
		Set<Charset> prioritisedCharsets = UrbanTextReporter.getPrioritisedCharsets();
		assertFalse(prioritisedCharsets.isEmpty());
		assertTrue("UTF8 should be supported by all systems", prioritisedCharsets.contains(StandardCharsets.UTF_8));
	}

}
