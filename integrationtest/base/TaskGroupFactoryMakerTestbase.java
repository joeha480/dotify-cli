package base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.daisy.streamline.api.tasks.TaskGroupFactoryMakerService;
import org.daisy.streamline.api.tasks.TaskGroupInformation;
import org.junit.Test;

@SuppressWarnings("javadoc")
public abstract class TaskGroupFactoryMakerTestbase {
	
	public abstract TaskGroupFactoryMakerService getTaskGroupFMS();

	@Test
	public void testFactoryExists() {
		//Setup
		TaskGroupFactoryMakerService factory = getTaskGroupFMS();
		
		//Test
		assertNotNull("Factory exists.", factory);
	}
	
	@Test
	public void testSupportedSpecifications() {
		//Setup
		TaskGroupFactoryMakerService factory = getTaskGroupFMS();
		Set<TaskGroupInformation> specs = factory.listAll();

		//Test
		assertEquals(8, specs.size());
		
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("text", "obfl").build()));
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("text", "xhtml").build()));
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("epub", "xhtml").build()));
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("obfl", "formatted-text").build()));
		
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("dtbook", "obfl").build()));
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("xml", "obfl").build()));
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("xhtml", "obfl").build()));
		assertTrue(specs.contains(TaskGroupInformation.newConvertBuilder("obfl", "pef").build()));

	}
	
	@Test
	public void testGetFactory() {
		//Setup
		TaskGroupFactoryMakerService factory = getTaskGroupFMS();
		
		//Test
		assertNotNull(factory.getFactory(TaskGroupInformation.newConvertBuilder("xml", "obfl").build()));
		assertNotNull(factory.getFactory(TaskGroupInformation.newConvertBuilder("text", "obfl").build()));
		assertNotNull(factory.getFactory(TaskGroupInformation.newConvertBuilder("dtbook", "obfl").build()));
	}

}