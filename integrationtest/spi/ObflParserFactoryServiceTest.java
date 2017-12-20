package spi;

import org.daisy.dotify.api.obfl.ObflParserFactoryMaker;
import org.daisy.dotify.api.obfl.ObflParserFactoryService;

import base.ObflParserFactoryServiceTestbase;

@SuppressWarnings("javadoc")
public class ObflParserFactoryServiceTest extends ObflParserFactoryServiceTestbase {

	@Override
	public ObflParserFactoryService getObflParserFS() {
		return ObflParserFactoryMaker.newInstance().getFactory();
	}

}