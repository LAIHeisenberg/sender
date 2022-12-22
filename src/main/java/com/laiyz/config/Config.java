package com.laiyz.config;

import com.laiyz.util.BFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
public class Config {

	private static String configFile = "config.properties";
	private static PropertiesConfiguration configs = new PropertiesConfiguration();

	private static File getConfigFile(String configFile) {
		URI uri = null;
		try {
			uri = Thread.currentThread().getContextClassLoader().getResource(configFile).toURI();
			return new File(uri);
		} catch (URISyntaxException e) {
			log.error("load config.properties error", e);
		}
		return null;
	}
	static {
		try {
			configs.read(new FileReader(getConfigFile(configFile)));
			configs.setSynchronizer(new ReadWriteSynchronizer());
		} catch (Exception ex) {
			log.error("parse config.properties error", ex);
		}
	}

	public static PropertiesConfiguration configs() {
		return configs;
	}

	public static final String SSL_ENABLED_KEY = "ssl.enabled";

	public static final String SERVER_PORT_KEY = "server.port";
	public static final String SERVER_HOST_KEY = "server.host";

	public static final String SERVER_DIR_KEY = "server.dir";
	public static final String CLIENT_DIR_KEY = "client.dir";

	public static final String FILE_TEMP_POSTFIX_KEY = "file.temp.postfix";

	public static final String SERVER_SSL_ROOT_FILE_KEY = "server.ssl.root_file";
	public static final String SERVER_SSL_KEY_FILE_KEY = "server.ssl.key_file";
	public static final String SERVER_SSL_CERT_CHAIN_FILE_KEY = "server.ssl.cert_chain_file";

	public static final String CLIENT_SSL_ROOT_FILE_KEY = "client.ssl.root_file";
	public static final String CLIENT_SSL_KEY_FILE_KEY = "client.ssl.key_file";
	public static final String CLIENT_SSL_CERT_CHAIN_FILE_KEY = "client.ssl.cert_chain_file";

	public static final String ENABLED = "Enabled";
	public static final String DISABLED = "Disabled";

	public static final String TRUE = "true";
	public static final String FALSE = "false";

	private static final String EMPTY_STR = "";
	/** ssl enabled */
	public static boolean sslEnabled() {return configs.getBoolean(SSL_ENABLED_KEY, false);}
	public static int port() {return configs.getInt(SERVER_PORT_KEY, 8899);}
	public static String host() {return configs.getString(SERVER_HOST_KEY, "0.0.0.0");}

	public static String serverDir() {return configs.getString(SERVER_DIR_KEY);}
	public static String clientDir() {return configs.getString(CLIENT_DIR_KEY);}

	public static String tempFilePostfix() {return configs.getString(FILE_TEMP_POSTFIX_KEY);}

	public static File serverSSLRootFile() {return readClasspathFile(configs.getString(SERVER_SSL_ROOT_FILE_KEY, EMPTY_STR));}
	public static File serverSSLKeyFile() {return readClasspathFile(configs.getString(SERVER_SSL_KEY_FILE_KEY, EMPTY_STR));}
	public static File serverSSLCertChainFile() {return readClasspathFile(configs.getString(SERVER_SSL_CERT_CHAIN_FILE_KEY, EMPTY_STR));}

	public static File clientSSLRootFile() {return readClasspathFile(configs.getString(CLIENT_SSL_ROOT_FILE_KEY, EMPTY_STR));}
	public static File clientSSLKeyFile() {return readClasspathFile(configs.getString(CLIENT_SSL_KEY_FILE_KEY, EMPTY_STR));}
	public static File clientSSLCertChainFile() {return readClasspathFile(configs.getString(CLIENT_SSL_CERT_CHAIN_FILE_KEY, EMPTY_STR));}

	private static final String CLASSPATH_PREFIX = "classpath:";
	private static File readClasspathFile(String path) {
		File file = null;
		if (StringUtils.isNotBlank(path) && path.startsWith(CLASSPATH_PREFIX)) {
			String classpath = path.substring(CLASSPATH_PREFIX.length());
			classpath = BFileUtil.convertToLocalePath(classpath);
			log.debug("path: {}, classpath: {}", path, classpath);
			URI uri = null;
			try {
				uri = Thread.currentThread().getContextClassLoader().getResource(classpath).toURI();
				log.debug("uri: {}", uri.toString());
			} catch (URISyntaxException e) {
				log.error("read classpath file error.", e);
			}
			file = new File(uri);
		}
		return file;
	}



	/**
	 * only update property in the cache, not save to file
	 * @param key
	 * @param value
	 */
	public static void setProperty(String key, Object value) {
		configs.setProperty(key, value);
	}

	/**
	 * update properties cache and save the update to disk(file)
	 * @param key
	 * @param value
	 */
	public static void setPropertyAndSave(String key, Object value) {
		setProperty(key, value);
		saveUpdate();
	}

	/**
	 * batch save the update to file(disk)
	 */
	public static void saveUpdate() {
		try {
			configs.getLayout().save(configs, new FileWriter(getConfigFile(configFile)));
		} catch (Exception e) {
			log.error("save config file update error", e);
		}
	}


	public static String test() { return configs.getString("test.a");}

	public static void main(String[] args) {
		System.out.println(test());
		setProperty("test.a", "YY12Y");
		setProperty("test.b", 113222);
		saveUpdate();
		System.out.println(test());
	}

}
