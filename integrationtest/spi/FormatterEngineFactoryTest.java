package spi;

import org.daisy.dotify.api.engine.FormatterEngineFactoryService;
import org.daisy.dotify.api.engine.FormatterEngineMaker;

import base.FormatterEngineFactoryTestbase;

@SuppressWarnings("javadoc")
public class FormatterEngineFactoryTest extends FormatterEngineFactoryTestbase {

	@Override
	public FormatterEngineFactoryService getFormatterEngineFS() {
		return FormatterEngineMaker.newInstance().getFactory();
	}

}