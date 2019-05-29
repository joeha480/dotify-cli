package osgi.config;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.io.File;
import java.net.MalformedURLException;

import org.ops4j.pax.exam.Option;

@SuppressWarnings("javadoc")
public abstract class ConfigurationOptions {
	
	public static Option felixDS() {
		return MavenRepo.CENTRAL.get("org.apache.felix", "org.apache.felix.scr", "2.1.14");
	}

	public static Option brailleUtilsCore() {
		return composite(
				dotifyApi(),
				MavenRepo.CENTRAL.get("org.daisy.libs", "jing", "20120724.0.0"),
				MavenRepo.CENTRAL.get("org.daisy.libs", "saxon-he", "9.5.1.5"),
				MavenRepo.CENTRAL.get("org.daisy.braille", "braille-utils.pef-tools", "6.0.0")
				);
	}
	
	public static Option brailleUtilsCatalog() {
		return MavenRepo.CENTRAL.get("org.daisy.braille", "braille-utils.impl", "7.0.0");
	}

	static Option dotifyApi() {
		return MavenRepo.CENTRAL.get("org.daisy.dotify", "dotify.api", "5.0.0");
	}
	
	static Option dotifyCommon() {
		return MavenRepo.CENTRAL.get("org.daisy.dotify", "dotify.common", "4.4.0");
	}
	
	public static Option dotifyText() {
		return composite(
				dotifyApi(),
				MavenRepo.CENTRAL.get("org.daisy.dotify", "dotify.text.impl", "5.0.0")
			);
	}
	
	static Option texhyphj() {
		return MavenRepo.CENTRAL.get("com.googlecode.texhyphj", "texhyphj", "1.2");
	}
	
	public static Option dotifyHyphenator() {
		return composite(
				dotifyApi(),
				dotifyCommon(),
				texhyphj(),
				MavenRepo.CENTRAL.get("org.daisy.dotify", "dotify.hyphenator.impl", "5.0.0")
			);
	}
	
	public static Option dotifyTranslator() {
		return composite(
				dotifyHyphenator(),
				MavenRepo.CENTRAL.get("net.java.dev.jna", "jna", "5.2.0"),
				MavenRepo.CENTRAL.get("org.liblouis", "liblouis-java", "4.1.0"),
				MavenRepo.SONATYPE_STAGING.get("org.daisy.dotify", "dotify.translator.impl", "5.1.0")
			);
	}
	
	static Option streamlineApi() {
		return MavenRepo.CENTRAL.get("org.daisy.streamline", "streamline-api", "1.5.0");
	}
	
	static Option jing() {
		return MavenRepo.CENTRAL.get("org.daisy.libs", "jing", "20120724.0.0");
	}
	
	static Option saxon() {
		return MavenRepo.CENTRAL.get("org.daisy.libs", "saxon-he", "9.5.1.5");
	}
	
	static Option stax2() {
		return MavenRepo.CENTRAL.get("org.codehaus.woodstox", "stax2-api", "3.1.4");
	}
	
	static Option wstx() {
		return composite(
				stax2(),
				MavenRepo.CENTRAL.get("com.fasterxml.woodstox", "woodstox-core", "5.0.2")
			);
				
	}
	
	public static Option dotifyFormatter() {
		return composite(
				streamlineApi(),
				dotifyText(), 
				dotifyHyphenator(), 
				dotifyTranslator(),
				wstx(),
				saxon(),
				MavenRepo.CENTRAL.get("org.daisy.dotify", "dotify.formatter.impl", "5.0.1")
			);
	}
	
	public static Option dotifyTasks() {
		return composite(
				dotifyApi(),
				streamlineApi(),
				dotifyCommon(),
				jing(),
				saxon(),
				MavenRepo.CENTRAL.get("org.daisy.dotify", "dotify.task.impl", "5.0.0")
			);
	}
	
	public static Option streamlineEngine() {
		return composite(
				dotifyCommon(),
				streamlineApi(),
				MavenRepo.CENTRAL.get("org.daisy.streamline", "streamline-engine", "1.3.0")
			);
	}

	enum MavenRepo {
		CENTRAL,
		SONATYPE_STAGING,
		LOCAL;
		
		Option get(String group, String artifact, String version) {
			switch(this) {
				case LOCAL:
					return local(group, artifact, version);
				case SONATYPE_STAGING:
					return sonatypeStaging(group, artifact, version);
				case CENTRAL: default:
					return mavenBundle().groupId(group).artifactId(artifact).version(version);
			}
		}
		
		static Option sonatypeStaging(String group, String artifact, String version) {
			String path = group.replaceAll("\\.", "/");
			return bundle("https://oss.sonatype.org/content/groups/staging/"+path+
					"/"+artifact+"/"+version+"/"+artifact+"-"+version+".jar");
		}
		
		static File mavenLocal = null;
		
		static synchronized File getMavenLocal() {
			if (mavenLocal==null) {
				File home = new File(System.getProperty("user.home"));
				mavenLocal = new File(new File(home, ".m2"), "repository");
				if (!mavenLocal.isDirectory()) {
					throw new RuntimeException("Cannot find maven local at " + mavenLocal);
				}			
			}
			return mavenLocal;
		}
		
		static Option local(String group, String artifact, String version) {
			try {
				String path = group.replaceAll("\\.", "/");
				String localPath = getMavenLocal().toURI().toURL().toExternalForm();
				return bundle(localPath+path+
						"/"+artifact+"/"+version+"/"+artifact+"-"+version+".jar");
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
