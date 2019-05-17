package org.daisy.dotify.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.daisy.dotify.api.embosser.EmbosserCatalog;
import org.daisy.dotify.api.factory.FactoryProperties;
import org.daisy.dotify.api.factory.FactoryProperties.ComparatorBuilder.SortProperty;
import org.daisy.dotify.api.hyphenator.HyphenatorFactoryMaker;
import org.daisy.dotify.api.paper.PaperCatalog;
import org.daisy.dotify.api.table.TableCatalog;
import org.daisy.dotify.api.translator.BrailleTranslatorFactoryMaker;
import org.daisy.dotify.api.translator.TranslatorSpecification;
import org.daisy.dotify.api.translator.TranslatorType;
import org.daisy.streamline.cli.Argument;
import org.daisy.streamline.cli.CommandDetails;
import org.daisy.streamline.cli.CommandParser;
import org.daisy.streamline.cli.CommandParserResult;
import org.daisy.streamline.cli.Definition;
import org.daisy.streamline.cli.ExitCode;
import org.daisy.streamline.cli.OptionalArgument;

class ListStuff implements CommandDetails {
	/**
	 * Prefix used for required arguments in the arguments map
	 */
	public static final String ARG_PREFIX = "required-";
	enum Mode {
		NAME,
		IDENTIFIER,
		NAME_IDENTIFIER
	};
	private final List<Argument> reqArgs;
	private final List<OptionalArgument> optionalArgs;
	private final CommandParser parser;
	
	private static final String EMBOSSERS_KEY = "embossers";
	private static final String TABLES_KEY = "tables";
	private static final String PAPERS_KEY = "papers";
	private static final String HYPHENATORS_KEY = "hyphenators";
	private static final String TRANSLATORS_KEY = "translators";
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
		defs.add(new Definition(TRANSLATORS_KEY, "to list available translators"));
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
		this.parser = CommandParser.create(this);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ListStuff ui = new ListStuff();
		if (args.length<1) {
			System.out.println("Expected at least one more argument.");
			System.out.println();
			ui.parser.displayHelp(System.out);
			ExitCode.MISSING_ARGUMENT.exitSystem();
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
			FactoryProperties[] ea = ec.listEmbossers().toArray(new FactoryProperties[]{});
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
			FactoryProperties[] ha = hyphs.listLocales().stream().map(loc -> {
					Locale l = Locale.forLanguageTag(loc);
					return new FactoryPropertiesAdapter(l.toLanguageTag(), l.getDisplayName());
				}).toArray(FactoryProperties[]::new);
			printList(ha, mode, prefix, separator, postfix);
		} else if (TRANSLATORS_KEY.equalsIgnoreCase(type)) {
			BrailleTranslatorFactoryMaker tr = BrailleTranslatorFactoryMaker.newInstance();
			Map<String, List<TranslatorSpecification>> map = tr.listSpecifications().stream()
					.filter(v->v.getModeDetails().getType().map(v2->v2!=TranslatorType.BYPASS).orElse(true))
				.reduce(new HashMap<String, List<TranslatorSpecification>>(),
					(x1, x2)->{
						List<TranslatorSpecification> s = x1.get(x2.getLocale());
						if (s==null) {
							s = new ArrayList<>();
							x1.put(x2.getLocale(), s);
						}
						s.add(x2);
						return x1;
					}, 
					(x1, x2)->{
						x1.putAll(x2);
						return x1;
					});
			FactoryProperties[] ta =map.keySet().stream()
					.sorted((v1, v2)->v1.compareTo(v2))
			.map(key->new FactoryPropertiesAdapter(key, map.get(key).stream().map(v->v.getModeDetails().getIdentifier()).collect(Collectors.joining("/"))))
			.toArray(FactoryProperties[]::new);
			printList(ta, mode, prefix, separator, postfix);
		}
	}
	
	private static class FactoryPropertiesAdapter implements FactoryProperties {
		private final String name, id;
		private FactoryPropertiesAdapter(String id, String name) {
			this.id = id;
			this.name = name;
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
		Arrays.sort(f, FactoryProperties.newComparatorBuilder().sortBy(SortProperty.IDENTIFIER).build());
	}
	
	private static void sortByName(FactoryProperties[] f) {
		Arrays.sort(f, FactoryProperties.newComparatorBuilder().sortBy(SortProperty.DISPLAY_NAME).build());
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
