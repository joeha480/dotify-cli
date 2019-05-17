package org.daisy.dotify.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.daisy.braille.utils.pef.PEFConverterFacade;
import org.daisy.braille.utils.pef.UnsupportedWidthException;
import org.daisy.dotify.Dotify;
import org.daisy.dotify.SystemKeys;
import org.daisy.dotify.api.embosser.EmbosserCatalog;
import org.daisy.dotify.api.embosser.EmbosserFactoryException;
import org.daisy.dotify.api.hyphenator.HyphenatorFactoryMaker;
import org.daisy.dotify.api.text.Integer2TextFactoryMaker;
import org.daisy.dotify.api.translator.BrailleTranslatorFactoryMaker;
import org.daisy.dotify.api.translator.TranslatorType;
import org.daisy.dotify.common.text.FilterLocale;
import org.daisy.dotify.tasks.impl.input.xml.XMLL10nResourceLocator;
import org.daisy.streamline.api.config.ConfigurationDetails;
import org.daisy.streamline.api.config.ConfigurationsCatalog;
import org.daisy.streamline.api.identity.IdentityProvider;
import org.daisy.streamline.api.media.AnnotatedFile;
import org.daisy.streamline.api.tasks.InternalTaskException;
import org.daisy.streamline.api.tasks.TaskSystemFactoryMaker;
import org.daisy.streamline.api.validity.Validator;
import org.daisy.streamline.api.validity.ValidatorFactoryMaker;
import org.daisy.streamline.cli.Argument;
import org.daisy.streamline.cli.CommandDetails;
import org.daisy.streamline.cli.CommandParser;
import org.daisy.streamline.cli.CommandParserResult;
import org.daisy.streamline.cli.Definition;
import org.daisy.streamline.cli.ExitCode;
import org.daisy.streamline.cli.OptionalArgument;
import org.daisy.streamline.cli.SwitchArgument;
import org.daisy.streamline.cli.SwitchMap;
import org.daisy.streamline.engine.DefaultTempFileWriter;
import org.xml.sax.SAXException;

/**
 * Provides a command line entry point to Dotify.
 * @author Joel Håkansson
 */
public class Convert implements CommandDetails {
	private static final Logger logger = Logger.getLogger(Convert.class.getCanonicalName());
	//private static final String DEFAULT_TEMPLATE = "A4-w32";
	private static final String DEFAULT_LOCALE = Locale.getDefault().toString().replaceAll("_", "-");
	private static final String CONFIG_KEY = "configs";
	private static final String CONFIG_WIKI_KEY = "config-wiki";
	private static final String WATCH_KEY = "watch";
	protected static final String META_KEY = "meta";
	
	private static final int DEFAULT_POLL_TIME = 5000;
	private static final int MIN_POLL_TIME = 250;

	private final List<Argument> reqArgs;
	private final List<OptionalArgument> optionalArgs;
	private final SwitchMap switches;
	private final BrailleUtilsInfo brailleInfo;
	private final CommandParser parser;

	public Convert() {
		this.brailleInfo = new BrailleUtilsInfo();
		//Use lazy loading of argument details
		this.reqArgs = new ArrayList<Argument>();
		this.optionalArgs = new ArrayList<OptionalArgument>();
		this.switches = new SwitchMap.Builder()
				.addSwitch(new SwitchArgument('w', WATCH_KEY, WATCH_KEY, "" + DEFAULT_POLL_TIME, "Keeps the conversion in sync by watching the input file for changes and rerunning the conversion automatically when the input is modified."))
				.addSwitch(new SwitchArgument('o', SystemKeys.LIST_OPTIONS, SystemKeys.LIST_OPTIONS, "true", "Lists additional options as the conversion runs."))
				.addSwitch(new SwitchArgument('c', CONFIG_KEY, META_KEY, CONFIG_KEY, "Lists known configurations."))
				.build();
		this.parser = CommandParser.create(this);
	}

	/**
	 * Provides a entry point for Dotify from the command line.
	 * @param args command line arguments
	 * @throws IOException if there is an i/o exception
	 * @throws InternalTaskException if there is a problem with running the tasks
	 */
	public static void main(String[] args) throws InternalTaskException, IOException {
		Convert m = new Convert();
		CommandParserResult result = m.parser.parse(args);
		List<String> p = result.getRequired();
		if (args.length<2 || p.size()<2) {
			if (CONFIG_KEY.equals(result.getOptional().get(META_KEY))) {
				System.out.println("Known configurations (locale, braille mode):");
				BrailleTranslatorFactoryMaker.newInstance().listSpecifications().stream()
					.filter(ts->ts.getModeDetails().getType().map(v2->v2!=TranslatorType.BYPASS&&v2!=TranslatorType.PRE_TRANSLATED).orElse(true))
					.sorted()
					.map(ts->"" + ts.getLocale() + ", " + ts.getMode())
					.forEach(System.out::println);
				ExitCode.OK.exitSystem();
			} else if (CONFIG_WIKI_KEY.equals(result.getOptional().get(META_KEY))) {
				System.out.println("Known configurations (locale, braille mode):");
				Set<String> uiLoc = new HashSet<>();
				uiLoc.add("en");
				uiLoc.add("sv");
				uiLoc.add("sv-SE");
				uiLoc.add("no");
				
				Set<String> textsLoc = XMLL10nResourceLocator.getInstance().listSupportedLocales();
				Set<String> writtenNumbersLoc = Integer2TextFactoryMaker.newInstance().listLocales().stream().collect(Collectors.toSet());
				
				Set<String> trLoc = BrailleTranslatorFactoryMaker.newInstance().listSpecifications().stream()
					.filter(ts->ts.getModeDetails().getType().map(v2->v2!=TranslatorType.BYPASS&&v2!=TranslatorType.PRE_TRANSLATED).orElse(true))
					.map(v->v.getLocale())
					.collect(Collectors.toSet());
				Set<String> hyphLoc = HyphenatorFactoryMaker.newInstance().listLocales()
						.stream()
						.collect(Collectors.toSet());
				Set<String> all = new HashSet<>(trLoc);
				all.addAll(hyphLoc);
				all.stream()
				.filter(v->!v.equals(Locale.forLanguageTag(v).getDisplayName(Locale.ENGLISH)))
				.sorted((v1, v2)->Locale.forLanguageTag(v1).getDisplayName(Locale.ENGLISH).compareTo(Locale.forLanguageTag(v2).getDisplayName(Locale.ENGLISH)))
				.map(v->String.format("|%s|%s|%s|%s|%s|%s|",
							Locale.forLanguageTag(v).getDisplayName(Locale.ENGLISH),
							(uiLoc.contains(v)?"&#x2713;":" "),
							(trLoc.contains(v)?"&#x2713;":" "),
							(hyphLoc.contains(v)?"&#x2713;":" "),
							(textsLoc.contains(v)?"&#x2713;":" "),
							(writtenNumbersLoc.contains(v)?"&#x2713;":" ")
						))
				.forEach(System.out::println);
				ExitCode.OK.exitSystem();
			} else {
				System.out.println("Expected at least two arguments");
				
				System.out.println();
				m.parser.displayHelp(System.out);
				ExitCode.MISSING_ARGUMENT.exitSystem();
			}
		} else if (p.size()>2) { 
			System.out.println("Unknown argument(s): " + p.subList(2, p.size()));
			System.out.println();
			m.parser.displayHelp(System.out);
			ExitCode.UNKNOWN_ARGUMENT.exitSystem();
		}
		// remove required arguments
		File input = new File(p.get(0));
		//File input = new File(args[0]);
		if (!input.exists()) {
			ExitCode.MISSING_RESOURCE.exitSystem("Cannot find input file: " + input);
		}
		
		final File output = new File(p.get(1)).getAbsoluteFile();

		final String context;
		{
			String s = result.getOptional().get("locale");
			if (s==null || s.equals("")) {
				s = DEFAULT_LOCALE;
			}
			context = s;
		}

		//File output = new File(args[1]);
		final HashMap<String, String> props = new HashMap<String, String>();
		//props.put("debug", "true");
		//props.put(SystemKeys.TEMP_FILES_DIRECTORY, TEMP_DIR);

		props.putAll(result.getOptional());
		
		if (input.isDirectory() && output.isDirectory()) {
			if (result.getOptional().get(WATCH_KEY)!=null) {
				logger.warning("'" + WATCH_KEY + "' is not implemented for batch mode.");
			}
			if ("true".equals(props.get(SystemKeys.WRITE_TEMP_FILES))) {
				ExitCode.ILLEGAL_ARGUMENT_VALUE.exitSystem("Cannot write debug files in batch mode.");
			}
			String format = props.get(SystemKeys.OUTPUT_FORMAT);
			if (format==null) {
				ExitCode.MISSING_ARGUMENT.exitSystem(SystemKeys.OUTPUT_FORMAT + " must be specified in batch mode.");
			} else if (format.equals(SystemKeys.PEF_FORMAT)) {
				format = "pef";
			} else if (format.equals(SystemKeys.TEXT_FORMAT)) {
				format = "txt";
			} else if (format.equals(SystemKeys.OBFL_FORMAT)) {
				format = "obfl";
			} else {
				ExitCode.ILLEGAL_ARGUMENT_VALUE.exitSystem("Unknown output format.");
			}
			//Experimental parallelization code in comment.
			//ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			final String ext = format;
			for (final File f : input.listFiles()) {
				//es.execute(new Runnable() {
					//public void run() {
						try {
					m.runDotify(f, new File(output, f.getName() + "." + ext), context, props);
						} catch (InternalTaskException e) {
							logger.log(Level.WARNING, "Failed to process " + f, e);
						} catch (IOException e) {
							logger.log(Level.WARNING, "Failed to read " + f, e);
						}
					//}});
			}
			//es.shutdown();
			//try {
			//	es.awaitTermination(600, TimeUnit.SECONDS);
			//} catch (InterruptedException e) {
			//	e.printStackTrace();
			//}
		} else if (input.isDirectory()) { 
			ExitCode.ILLEGAL_ARGUMENT_VALUE.exitSystem("If input is a directory, output must be an existing directory too.");
		} else {
			String pollWaitStr = result.getOptional().get(WATCH_KEY);
			if (pollWaitStr!=null) {
				int pollWait = DEFAULT_POLL_TIME;
				try {
					pollWait = Math.max(Integer.parseInt(pollWaitStr), MIN_POLL_TIME);
				} catch (NumberFormatException e) {
					logger.warning("Could not parse " + WATCH_KEY + " value '" + pollWaitStr + "' as an integer.");
				}
				logger.fine("Poll time is " + pollWait);
				long modified = 0;
				while (input.exists()) {
					if (modified<input.lastModified()) {
						modified = input.lastModified();
						try {
							//delete the output so that it is not there if something goes wrong
							output.delete();
							m.runDotify(input, output, context, props);
						} catch (Exception e) { 
							logger.log(Level.SEVERE, "A severe error occurred.", e);
						}
						logger.info("Waiting for changes in " + input);
					}
					try {
						Thread.sleep(pollWait);
					} catch (InterruptedException e) {
					}
				}
			} else {
				m.runDotify(input, output, context, props);
			}
		}
	}
	
	private void runDotify(File input, File output, String context, HashMap<String, String> props) throws InternalTaskException, IOException {
		if (!input.exists()) {
			ExitCode.MISSING_RESOURCE.exitSystem("Cannot find input file: " + input);
		}
		Dotify.run(input, output, FilterLocale.parse(context), props);
		if (output.exists()) {
			AnnotatedFile ao = IdentityProvider.newInstance().identify(output);
			String mediaType = ao.getMediaType();
			ValidatorFactoryMaker validatorFactory = ValidatorFactoryMaker.newInstance();
			Validator validator;
			if (mediaType!=null && (validator = validatorFactory.newValidator(mediaType))!=null) {
				logger.info(String.format("Validating output using %s", validator.getClass().getName()));
				if (!validator.validate(output.toURI().toURL()).isValid()) {
					logger.warning("Validation failed: " + output);
				} else {
					logger.info("Output is valid.");
					if (mediaType.equals("application/x-pef+xml") && props.containsKey(PEFConverterFacade.KEY_TABLE)) {
						// create brl
						HashMap<String, String> p = new HashMap<String, String>();
						p.put(PEFConverterFacade.KEY_TABLE, props.get(PEFConverterFacade.KEY_TABLE));
						try {
							brailleInfo.getShortFormResolver().expandShortForm(p, PEFConverterFacade.KEY_TABLE);
						} catch (IllegalArgumentException e) {
							ExitCode.ILLEGAL_ARGUMENT_VALUE.exitSystem(e.getMessage());
						}
						File f = new File(output.getParentFile(), output.getName() + ".brl");
						logger.info("Writing brl to " + f.getAbsolutePath());
						try (FileOutputStream os = new FileOutputStream(f)) {
							new PEFConverterFacade(EmbosserCatalog.newInstance()).parsePefFile(output, os, null, p);
						} catch (ParserConfigurationException e) {
							logger.log(Level.FINE, "Parse error when converting to brl", e);
						} catch (SAXException e) {
							logger.log(Level.FINE, "SAX error when converting to brl", e);
						} catch (UnsupportedWidthException e) {
							logger.log(Level.FINE, "Width error when converting to brl", e);
						} catch (NumberFormatException e) {
							logger.log(Level.FINE, "Number format error when converting to brl", e);
						} catch (EmbosserFactoryException e) {
							logger.log(Level.FINE, "Embosser error when converting to brl", e);
						}
					}
				}
			}
		}
	}

	@Override
	public String getName() {
		return DotifyCLI.CONVERT;
	}
	
	@Override
	public String getDescription() {
		return "Converts documents into braille.";
	}

	@Override
	public List<Argument> getRequiredArguments() {
		if (reqArgs.isEmpty()) {
			TaskSystemFactoryMaker fm = TaskSystemFactoryMaker.newInstance();
			//TODO: map identifiers to file formats
			Set<String> inputFormats = fm.listInputs().stream().map(v->v.getIdentifier()).collect(Collectors.toSet());
			Set<String> outputFormats = fm.listOutputs().stream().map(v->v.getIdentifier()).collect(Collectors.toSet());
			reqArgs.add(new Argument("path_to_input", "Path to the input file " + inputFormats));
			reqArgs.add(new Argument("path_to_output", "Path to the output file " + outputFormats));
		}
		return reqArgs;
	}

	@Override
	public List<OptionalArgument> getOptionalArguments() {
		if (optionalArgs.isEmpty()) {
			{
				ArrayList<Definition> vals = new ArrayList<Definition>();
				ConfigurationsCatalog c = ConfigurationsCatalog.newInstance();
				List<ConfigurationDetails> detailsList = c.getConfigurationDetails().stream()
						.sorted((o1, o2) -> {
							return o1.getKey().compareTo(o2.getKey());
						})
						.collect(Collectors.toList());
				for (ConfigurationDetails details : detailsList) {
					vals.add(new Definition(details.getKey(), details.getDescription()));
				}
				vals.add(new Definition("[other]", "Path to setup file"));
				optionalArgs.add(new OptionalArgument("preset", "A preset to use", vals, null));
			}
			optionalArgs.add(new OptionalArgument("locale", "The target locale for the result", DEFAULT_LOCALE));
			
			{
				ArrayList<Definition> vals = new ArrayList<Definition>();
				vals.add(new Definition(SystemKeys.PEF_FORMAT, "write result in PEF-format"));
				vals.add(new Definition(SystemKeys.TEXT_FORMAT, "write result as text"));
				//vals.add(new Definition(SystemKeys.OBFL_FORMAT, "write result in OBFL-format (bypass formatter)"));
				optionalArgs.add(new OptionalArgument(SystemKeys.OUTPUT_FORMAT, "Specifies output format", vals, "[detect]"));
			}
			{
				ArrayList<Definition> vals = new ArrayList<Definition>();
				vals.add(new Definition("true", "outputs temp files"));
				vals.add(new Definition("false", "does not output temp files"));
				optionalArgs.add(new OptionalArgument(SystemKeys.WRITE_TEMP_FILES, "Writes temp files", vals, "false"));
			}
			optionalArgs.add(new OptionalArgument(SystemKeys.TEMP_FILES_DIRECTORY, "Path to temp files directory", DefaultTempFileWriter.TEMP_DIR));
			optionalArgs.add(new OptionalArgument(PEFConverterFacade.KEY_TABLE, "If specified, an ASCII-braille file (.brl) is generated in addition to the PEF-file using the specified braille code table", brailleInfo.getDefinitionList(), ""));
		}
		return optionalArgs;
	}

	@Override
	public SwitchMap getSwitches() {
		return switches;
	}

}
