/**
 * 
 */
package com.github.ansell.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.github.ansell.csv.stream.CSVStream;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Engineers solutions where possible to interconversion between character
 * encodings.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public final class UrbanTextEngineer {

	public static void main(String... args) throws Exception {
		final OptionParser parser = new OptionParser();

		final OptionSpec<Void> help = parser.accepts("help").forHelp();
		final OptionSpec<String> inputCharsetOption = parser.accepts("input-charset").withRequiredArg()
				.ofType(String.class).required().describedAs(
						"The input charset to use. There are no defaults, this must be specified. Use urban-text to get clues about which charset to use.");
		final OptionSpec<String> outputCharsetOption = parser.accepts("output-charset").withRequiredArg()
				.ofType(String.class).required().describedAs("The output charset to use.");
		final OptionSpec<File> inputOption = parser.accepts("input").withRequiredArg().ofType(File.class)
				.describedAs("The input file to be fixed. Will attempt to use standard in if none is specified.");
		final OptionSpec<File> outputOption = parser.accepts("output").withRequiredArg().ofType(File.class)
				.describedAs("The file to contain the output. Will attempt to use standard out if none is specified.");

		OptionSet options = null;

		try {
			options = parser.parse(args);
		} catch (final OptionException e) {
			System.out.println(e.getMessage());
			parser.printHelpOn(System.out);
			throw e;
		}

		if (options.has(help)) {
			parser.printHelpOn(System.out);
			return;
		}

		final StringWriter reportOutput = new StringWriter();
		Path tempInputFile = null;
		Path tempOutputFile = null;
		try {
			final Path inputPath;
			if (options.has(inputOption)) {
				inputPath = inputOption.value(options).toPath();
				if (!Files.exists(inputPath)) {
					throw new FileNotFoundException("Could not find input file: " + inputPath.toString());
				}
			} else {
				inputPath = tempInputFile = Files.createTempFile("UrbanTextInput-", ".txt");
				Files.copy(System.in, tempInputFile, StandardCopyOption.REPLACE_EXISTING);
			}

			final Path outputPath;
			if (options.has(outputOption)) {
				outputPath = outputOption.value(options).toPath();
				if (Files.exists(outputPath)) {
					throw new FileNotFoundException(
							"Output file already exists, not overwriting: " + outputPath.toString());
				}
			} else {
				outputPath = tempOutputFile = Files.createTempFile("UrbanTextOutput-", ".txt");
			}

			final Charset inputCharset = Charset.forName(inputCharsetOption.value(options));
			final Charset outputCharset = Charset.forName(outputCharsetOption.value(options));
			// Setup reporter to exit if there is an issue,
			boolean exitOnFirstError = true;
			UrbanTextReporter.runReporter(inputPath, Collections.singleton(inputCharset), reportOutput,
					exitOnFirstError);
			CharBuffer inBuffer = UrbanTextReporter.decodeFile(inputPath, inputCharset);
			UrbanTextReporter.encodeFile(inBuffer, outputPath, outputCharset);
			if (tempOutputFile != null) {
				Files.copy(tempOutputFile, System.out);
			}
		} finally {
			if (tempInputFile != null) {
				Files.deleteIfExists(tempInputFile);
			}
			if (tempOutputFile != null) {
				Files.deleteIfExists(tempOutputFile);
			}
		}
	}

	/**
	 * Private constructor for static-only class
	 */
	private UrbanTextEngineer() {
	}

}
