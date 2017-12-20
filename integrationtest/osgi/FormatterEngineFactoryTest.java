package osgi;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;

import javax.inject.Inject;

import org.daisy.dotify.api.engine.FormatterEngineFactoryService;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;

import base.FormatterEngineFactoryTestbase;
import osgi.config.ConfigurationOptions;

@SuppressWarnings("javadoc")
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class FormatterEngineFactoryTest extends FormatterEngineFactoryTestbase {

	@Configuration
	public Option[] configure() {
		return options(
			ConfigurationOptions.felixDS(),
			ConfigurationOptions.dotifyFormatter(),
			junitBundles()
		);
	}

	@Inject @Filter(timeout=5000)
	FormatterEngineFactoryService factory;

	@Override
	public FormatterEngineFactoryService getFormatterEngineFS() {
		return factory;
	}
}
