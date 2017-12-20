package base;

import static org.junit.Assert.assertNotNull;

import org.daisy.dotify.api.obfl.ObflParserFactoryService;
import org.junit.Test;

@SuppressWarnings("javadoc")
public abstract class ObflParserFactoryServiceTestbase {
	
	public abstract ObflParserFactoryService getObflParserFS();

	@Test
	public void testFactoryExists() {
		//Setup
		ObflParserFactoryService factory = getObflParserFS();
		
		//Test
		assertNotNull("Factory exists.", factory);
	}

}