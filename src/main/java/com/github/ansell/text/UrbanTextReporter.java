/**
 * 
 */
package com.github.ansell.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
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
 * Investigates which text encodings a file may be living in and reports back.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public final class UrbanTextReporter {

	public static void main(String... args) throws Exception {
		final OptionParser parser = new OptionParser();

		final OptionSpec<Void> help = parser.accepts("help").forHelp();
		final OptionSpec<File> input = parser.accepts("input").withRequiredArg().ofType(File.class).describedAs(
				"The input file to be investigated. Will attempt to use standard in if none is specified.");
		final OptionSpec<File> output = parser.accepts("output").withRequiredArg().ofType(File.class)
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

		Path tempFile = null;
		try {
			final Path inputPath;
			if (options.has(input)) {
				inputPath = input.value(options).toPath();
				if (!Files.exists(inputPath)) {
					throw new FileNotFoundException("Could not find input file: " + inputPath.toString());
				}
			} else {
				inputPath = tempFile = Files.createTempFile("UrbanTextInput-", ".txt");
				Files.copy(System.in, tempFile, StandardCopyOption.REPLACE_EXISTING);
			}

			final Writer outputWriter;
			if (options.has(output)) {
				final Path outputPath = output.value(options).toPath();
				if (Files.exists(outputPath)) {
					throw new FileNotFoundException(
							"Output file already exists, not overwriting: " + outputPath.toString());
				}
				outputWriter = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8,
						StandardOpenOption.CREATE_NEW);
			} else {
				outputWriter = new PrintWriter(System.out);
			}

			final Set<Charset> prioritisedCharsets = getPrioritisedCharsets();
			try {
				runReporter(inputPath, prioritisedCharsets, outputWriter);
			} finally {
				outputWriter.close();
			}
		} finally {
			if (tempFile != null) {
				Files.deleteIfExists(tempFile);
			}
		}
	}

	public static void runReporter(final Path inputPath, Set<Charset> prioritisedCharsets, Writer outputWriter)
			throws IOException {
		runReporter(inputPath, prioritisedCharsets, outputWriter, false);
	}

	public static void runReporter(final Path inputPath, Set<Charset> prioritisedCharsets, Writer outputWriter,
			boolean exitOnFirstError) throws IOException {
		try (final RandomAccessFile randomAccessFile = new RandomAccessFile(inputPath.toFile(), "r");
				final FileChannel inChannel = randomAccessFile.getChannel();
				final SequenceWriter csvWriter = CSVStream.newCSVWriter(outputWriter,
						Arrays.asList("Charset", "EncoderError", "Position", "BytesContext"));) {
			MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
			for (Charset nextCharset : prioritisedCharsets) {
				try {
					inBuffer.rewind();
					decodeBuffer(inBuffer, nextCharset);
					csvWriter.write(Arrays.asList(nextCharset.name(), "", "", ""));
				} catch (CharacterCodingException e) {
					// System.out.print("Character coding error at byte
					// position: ");
					// System.out.println(inBuffer.position());
					// System.out.println(e.getMessage());
					StringBuilder nextBytes = getDebugBytesFromBuffer(inBuffer);
					// System.out.println(nextBytes.toString());
					csvWriter.write(Arrays.asList(nextCharset.name(), e.getMessage(),
							Integer.toString(inBuffer.position()), nextBytes.toString()));
					if (exitOnFirstError) {
						throw e;
					}
				} catch (IOException e) {
					// Other IO Exceptions are not likely to be recoverable, so
					// abort early rather than continuing
					throw e;
				} catch (Exception e) {
					csvWriter.write(Arrays.asList(nextCharset.name(), e.getMessage(),
							Integer.toString(inBuffer.position()), ""));
					if (exitOnFirstError) {
						throw e;
					}
				}
			}
		}
	}

	public static StringBuilder getDebugBytesFromBuffer(ByteBuffer inBuffer) {
		StringBuilder nextBytes = new StringBuilder();
		if (inBuffer.position() > 1) {
			nextBytes.append(String.format("%02X ", inBuffer.get(inBuffer.position() - 2)));
		}
		if (inBuffer.position() > 0) {
			nextBytes.append(String.format("%02X ", inBuffer.get(inBuffer.position() - 1)));
		}
		nextBytes.append(String.format("[%02X] ", inBuffer.get(inBuffer.position())));
		if (inBuffer.position() < inBuffer.limit() - 1) {
			nextBytes.append(String.format("%02X ", inBuffer.get(inBuffer.position() + 1)));
		}
		if (inBuffer.position() < inBuffer.limit() - 2) {
			nextBytes.append(String.format("%02X ", inBuffer.get(inBuffer.position() + 2)));
		}
		return nextBytes;
	}

	public static void encodeFile(final CharBuffer inBuffer, final Path outputPath, final Charset nextCharset)
			throws IOException {
		CharsetEncoder charsetEncoder = nextCharset.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try (final RandomAccessFile randomAccessFile = new RandomAccessFile(outputPath.toFile(), "rw");
				final FileChannel outChannel = randomAccessFile.getChannel();) {
			outChannel.write(charsetEncoder.encode(inBuffer));
		}
	}

	public static CharBuffer decodeFile(final Path inputPath, Charset nextCharset) throws IOException {
		try (final RandomAccessFile randomAccessFile = new RandomAccessFile(inputPath.toFile(), "r");
				final FileChannel inChannel = randomAccessFile.getChannel();) {
			MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
			inBuffer.rewind();
			return decodeBuffer(inBuffer, nextCharset);
		}
	}

	public static CharBuffer decodeBuffer(MappedByteBuffer inBuffer, Charset nextCharset)
			throws CharacterCodingException {
		CharsetDecoder charsetDecoder = nextCharset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		return charsetDecoder.decode(inBuffer);
	}

	public static Set<Charset> getPrioritisedCharsets() {
		Set<Charset> prioritisedCharsets = new LinkedHashSet<>();

		// Add some likely candidates at the top of the list so they appear at
		// the top of the resulting report
		prioritisedCharsets.add(StandardCharsets.UTF_8);
		prioritisedCharsets.add(StandardCharsets.US_ASCII);
		prioritisedCharsets.add(StandardCharsets.ISO_8859_1);
		prioritisedCharsets.add(StandardCharsets.UTF_16);
		prioritisedCharsets.add(Charset.defaultCharset());

		// Sort all others for a semi-consistent output (except for
		// defaultCharset above which may change for different systems
		List<Charset> sortedOthers = new ArrayList<>();
		for (Entry<String, Charset> nextCharsetEntry : Charset.availableCharsets().entrySet()) {
			prioritisedCharsets.add(nextCharsetEntry.getValue());
		}
		Collections.sort(sortedOthers);
		prioritisedCharsets.addAll(sortedOthers);
		return prioritisedCharsets;
	}

	/**
	 * Private constructor for static-only class
	 */
	private UrbanTextReporter() {
	}

}
