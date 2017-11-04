package org.daisy.dotify.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.daisy.braille.utils.api.embosser.EmbosserCatalog;
import org.daisy.braille.utils.api.factory.FactoryProperties;
import org.daisy.braille.utils.api.factory.FactoryPropertiesComparator;
import org.daisy.braille.utils.api.factory.FactoryPropertiesComparator.By;
import org.daisy.braille.utils.api.paper.PaperCatalog;
import org.daisy.braille.utils.api.table.TableCatalog;
import org.daisy.dotify.consumer.hyphenator.HyphenatorFactoryMaker;
import org.daisy.streamline.cli.AbstractUI;
import org.daisy.streamline.cli.Argument;
import org.daisy.streamline.cli.CommandParserResult;
import org.daisy.streamline.cli.Definition;
import org.daisy.streamline.cli.ExitCode;
import org.daisy.streamline.cli.OptionalArgument;

class ListStuff extends AbstractUI {
	enum Mode {
		NAME,
		IDENTIFIER,
		NAME_IDENTIFIER
	};
	private final List<Argument> reqArgs;
	private final List<OptionalArgument> optionalArgs;
	
	private static final String EMBOSSERS_KEY = "embossers";
	private static final String TABLES_KEY = "tables";
	private static final String PAPERS_KEY = "papers";
	private static final String HYPHENATORS_KEY = "hyphenators";
	private static final String MODE_KEY = "mode";
	private static final String PREFIX_KEY = "prefix";
	private static final String POSTFIX_KEY = "postfix";
	private static final String SEPARATOR_KEY = "separator";
	
	public ListStuff() {
		reqArgs = new ArrayList<Argument>();
		ArrayList<Definition> defs = new ArrayList<Definition>();
		defs.add(new Definition(EMBOSSERS_KEY, "to list available embossers"));
		defs.add(new Definition(TABLES_KEY, "to list available tables"));
		defs.add(new Definition(PAPERS_KEY, "to list available papers"));
		defs.add(new Definition(HYPHENATORS_KEY, "to list available hyphenators"));
		reqArgs.add(new Argument("type_of_objects", "What to list", defs));
		optionalArgs = new ArrayList<OptionalArgument>();
		ArrayList<Definition> modes = new ArrayList<Definition>();
		modes.add(new Definition(Mode.NAME.toString(), "List display names"));
		modes.add(new Definition(Mode.IDENTIFIER.toString(), "List identifiers"));
		modes.add(new Definition(Mode.NAME_IDENTIFIER.toString(), "List names followed by identifier"));
		optionalArgs.add(new OptionalArgument(MODE_KEY, "Mode", modes, Mode.NAME.toString()));
		optionalArgs.add(new OptionalArgument(PREFIX_KEY, "Line prefix.", ""));
		optionalArgs.add(new OptionalArgument(POSTFIX_KEY, "Line postfix.", ""));
		optionalArgs.add(new OptionalArgument(SEPARATOR_KEY, "Field separator. Only used when there is more than one field on each line.", ""));
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ListStuff ui = new ListStuff();
		if (args.length<1) {
			System.out.println("Expected at least one more argument.");
			System.out.println();
			ui.displayHelp(System.out);
			System.exit(-ExitCode.MISSING_ARGUMENT.ordinal());
		}
		String type;
		String prefix;
		String postfix;
		String separator;
		Mode mode = Mode.NAME;
		{
			CommandParserResult cp = ui.parser.parse(args);
			Map<String, String> p = cp.toMap(ARG_PREFIX);
			type = p.remove(ARG_PREFIX+0);
			Map<String, String> op = cp.getOptional();
			prefix = replaceNullWithEmpty(op.get(PREFIX_KEY));
			postfix = replaceNullWithEmpty(op.get(POSTFIX_KEY));
			separator = replaceNullWithEmpty(op.get(SEPARATOR_KEY));
			String modeStr = op.get(MODE_KEY);
			if (modeStr!=null) {
				try {
					mode = Mode.valueOf(modeStr.toUpperCase());
				} catch (Exception e) {}
			}
		}
		System.out.println();
		if (EMBOSSERS_KEY.equalsIgnoreCase(type)) {
			EmbosserCatalog ec = EmbosserCatalog.newInstance();
			FactoryProperties[] ea = ec.list().toArray(new FactoryProperties[]{});
			printList(ea, mode, prefix, separator, postfix);
		} else if (TABLES_KEY.equalsIgnoreCase(type)) {
			TableCatalog tc = TableCatalog.newInstance();
			FactoryProperties[] ta = tc.list().toArray(new FactoryProperties[]{});
			printList(ta, mode, prefix, separator, postfix);
		} else if (PAPERS_KEY.equalsIgnoreCase(type)) {
			PaperCatalog pc = PaperCatalog.newInstance();
			FactoryProperties[] pa = pc.list().toArray(new FactoryProperties[]{});
			printList(pa, mode, prefix, separator, postfix);
		} else if (HYPHENATORS_KEY.equalsIgnoreCase(type)) {
			HyphenatorFactoryMaker hyphs = HyphenatorFactoryMaker.newInstance();
			FactoryProperties[] ha = hyphs.listLocales().stream().map(loc -> new LocaleFactoryPropertiesAdapter(Locale.forLanguageTag(loc))).toArray(FactoryProperties[]::new);
			printList(ha, mode, prefix, separator, postfix);
		}
	}
	
	private static class LocaleFactoryPropertiesAdapter implements FactoryProperties {
		private final String name, id;
		private LocaleFactoryPropertiesAdapter(Locale l) {
			this.id = l.toLanguageTag();
			this.name = Arrays.asList(l.getDisplayLanguage(), l.getDisplayCountry()).stream().filter(v -> !v.isEmpty()).collect(Collectors.joining(", "));
		}
		@Override
		public String getIdentifier() {
			return id;
		}
		@Override
		public String getDisplayName() {
			return name;
		}
		@Override
		public String getDescription() {
			return "";
		}
	}
	
	private static String replaceNullWithEmpty(String input) {
		if (input==null) {
			return "";
		} else {
			return input;
		}
	}
	
	private static void printList(FactoryProperties[] f, Mode mode, String prefix, String separator, String postfix) {
		switch (mode) {
			case NAME:
				sortByName(f);
				for (FactoryProperties p : f) {
					System.out.println(prefix + p.getDisplayName() + postfix);
				}
				break;
			case IDENTIFIER:
				sortById(f);
				for (FactoryProperties p : f) {
					System.out.println(prefix + p.getIdentifier() + postfix);
				}
				break;
			case NAME_IDENTIFIER:
				sortByName(f);
				for (FactoryProperties p : f) {
					System.out.println(prefix + p.getDisplayName() + separator + p.getIdentifier() + postfix);
				}
		}
	}
	
	private static void sortById(FactoryProperties[] f) {
		Arrays.sort(f, new FactoryPropertiesComparator().by(By.IDENTIFIER));
	}
	
	private static void sortByName(FactoryProperties[] f) {
		Arrays.sort(f, new FactoryPropertiesComparator().by(By.DISPLAY_NAME));
	}

	@Override
	public List<Argument> getRequiredArguments() {
		return reqArgs;
	}

	@Override
	public List<OptionalArgument> getOptionalArguments() {
		return optionalArgs;
	}

	@Override
	public String getName() {
		return DotifyCLI.LIST;
	}
	
	@Override
	public String getDescription() {
		return "Lists available implementations of a specific type.";
	}

}
