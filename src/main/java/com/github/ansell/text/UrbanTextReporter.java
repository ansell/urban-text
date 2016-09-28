/**
 * 
 */
package com.github.ansell.text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Investigates which text encodings a file may be living in and reports back.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class UrbanTextReporter {

	public static void main(String... args) throws Exception {
		final OptionParser parser = new OptionParser();

		final OptionSpec<Void> help = parser.accepts("help").forHelp();
		final OptionSpec<File> input = parser.accepts("input").withRequiredArg().ofType(File.class).required()
				.describedAs("The input file to be investigated.");

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

		final Path inputPath = input.value(options).toPath();
		if (!Files.exists(inputPath)) {
			throw new FileNotFoundException("Could not find input file: " + inputPath.toString());
		}

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

		Map<Charset, String> results = new LinkedHashMap<>();

		for (Charset nextCharset : prioritisedCharsets) {
			try (FileChannel inChannel = new RandomAccessFile(inputPath.toFile(), "r").getChannel();) {
				MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
				nextCharset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT).decode(inBuffer);
				results.put(nextCharset, "");
			} catch (Exception e) {
				results.put(nextCharset, e.getMessage());
			}
		}
	}

}
