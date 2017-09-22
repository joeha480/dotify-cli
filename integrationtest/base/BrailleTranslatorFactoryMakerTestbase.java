package base;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.daisy.dotify.api.translator.BrailleTranslator;
import org.daisy.dotify.api.translator.BrailleTranslatorFactory;
import org.daisy.dotify.api.translator.BrailleTranslatorFactoryMakerService;
import org.daisy.dotify.api.translator.Translatable;
import org.daisy.dotify.api.translator.TranslationException;
import org.daisy.dotify.api.translator.TranslatorConfigurationException;
import org.junit.Test;

@SuppressWarnings("javadoc")
public abstract class BrailleTranslatorFactoryMakerTestbase {
	
	public abstract BrailleTranslatorFactoryMakerService getBrailleTranslatorFMS();

	@Test
	public void testTranslatorFactory() {
		//Setup
		BrailleTranslatorFactoryMakerService translatorFactory = getBrailleTranslatorFMS();
		//Test
		assertNotNull("Factory exists.", translatorFactory);
		assertTrue(translatorFactory.listSpecifications().size()>=62);
	}
	
	@Test
	public void testSwedishUncontractedTranslator() throws TranslatorConfigurationException, TranslationException {
		//Setup
		BrailleTranslatorFactoryMakerService translatorFactory = getBrailleTranslatorFMS();
		BrailleTranslator bt = translatorFactory.newTranslator("sv-SE", BrailleTranslatorFactory.MODE_UNCONTRACTED);
		//Test
		assertNotNull(bt);
		assertEquals("⠼⠁⠃⠉", bt.translate(Translatable.text("123").build()).getTranslatedRemainder());
	}

	@Test
	public void testEnglishBypassTranslator() throws TranslatorConfigurationException, TranslationException {
		// Setup
		BrailleTranslatorFactoryMakerService translatorFactory = getBrailleTranslatorFMS();
		BrailleTranslator bt = translatorFactory.newTranslator("en", BrailleTranslatorFactory.MODE_BYPASS);
		// Test
		assertNotNull(bt);
		assertEquals("123", bt.translate(Translatable.text("123").build()).getTranslatedRemainder());
	}
}
