package base;

import static org.junit.Assert.assertNotNull;

import org.daisy.dotify.api.engine.FormatterEngineFactoryService;
import org.junit.Test;

@SuppressWarnings("javadoc")
public abstract class FormatterEngineFactoryTestbase {
	
	public abstract FormatterEngineFactoryService getFormatterEngineFS();

	@Test
	public void testFactoryExists() {
		//Setup
		FormatterEngineFactoryService factory = getFormatterEngineFS();
		
		//Test
		assertNotNull("Factory exists.", factory);
	}

}