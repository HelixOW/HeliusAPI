package io.github.alphahelixdev.helius;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.alphahelixdev.helius.cipher.HeliusCipher;
import io.github.alphahelixdev.helius.file.json.JsonReadFile;
import io.github.alphahelixdev.helius.reflection.Reflections;
import io.github.alphahelixdev.helius.sql.SQLInformation;
import io.github.alphahelixdev.helius.sql.SQLTableHandler;
import io.github.alphahelixdev.helius.sql.exceptions.NoConnectionException;
import io.github.alphahelixdev.helius.sql.mysql.MySQLConnector;
import io.github.alphahelixdev.helius.sql.sqlite.SQLiteConnector;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Helius {
	
	private static final Set<Cache> CACHES = new HashSet<>();
	private static final HeliusCipher CIPHER = new HeliusCipher();
	private static final Reflections REFLECTIONS = new Reflections();
	private static final String HOME_PATH = System.getProperty("user.home") + "/storage";
	private static Helius instance;
	private static Logger log;
	
	static {
		log = Logger.getLogger("Helius");
		System.setProperty("java.util.logging.SimpleFormatter.format",
				"[%1$tF %1$tT] [%4$-7s]: %5$s %n");
		
		try {
			File logFile = createFile(new File(HOME_PATH + "/helius/helius.log"));
			
			FileHandler fh = new FileHandler(logFile.getAbsolutePath());
			log.addHandler(fh);
			
			fh.setFormatter(new Formatter() {
				@Override
				public String format(LogRecord record) {
					SimpleDateFormat logTime = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
					Calendar cal = new GregorianCalendar();
					cal.setTimeInMillis(record.getMillis());
					return "[" + logTime.format(cal.getTime()) + "] [" + record.getLevel() + "]: "
							+ record.getMessage() + "\n";
				}
			});
		} catch(SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	Helius() {
		JsonReadFile appFile = new JsonReadFile(HOME_PATH + "/helius/apps.json");
		
		for(JsonElement obj : appFile.getArrayValues()) {
			if(!obj.isJsonObject()) continue;
			JsonObject info = (JsonObject) obj;
			
			try {
				loadJar(new File(info.get("jar").getAsString()), info.get("main").getAsString()).newInstance();
				log.info("Loaded " + info.get("name").getAsString());
			} catch(ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
		
		log.info("Helius has been loaded!");
		startCacheClearTask();
	}
	
	public static Class<?> loadJar(File jarFile, String mainClass) throws ClassNotFoundException {
		return loadJar(jarFile, mainClass, Helius.class.getClassLoader());
	}
	
	private static void startCacheClearTask() {
		Timer timer = new Timer();
		
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for(Cache c : CACHES) {
					c.clear();
					log.info(c.clearMessage());
				}
			}
		}, Cache.time * 1000 * 60, Cache.time * 1000 * 60);
	}
	
	public static <T extends ClassLoader> Class<?> loadJar(File jarFile, String mainClass, T classLoader) throws ClassNotFoundException {
		for(Class<?> classes : loadJar(jarFile, classLoader)) {
			if(classes.getName().equals(mainClass))
				return classes;
		}
		throw new ClassNotFoundException("Unable to find the main class '" + mainClass + "' inside '" + jarFile + "'");
	}
	
	public static <T extends ClassLoader> Set<Class<?>> loadJar(File jarFile, T classLoader) {
		Set<Class<?>> classes = new HashSet<>();
		
		try {
			URLClassLoader loader;
			
			if(classLoader != null)
				loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, classLoader);
			else
				loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()});
			
			JarFile file = new JarFile(jarFile);
			
			for(Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements(); ) {
				JarEntry entry = entries.nextElement();
				String jarName = entry.getName().replace('/', '.');
				
				if(jarName.endsWith(".class")) {
					String clName = jarName.substring(0, jarName.length() - 6);
					
					if(clName.startsWith("io.github.alphahelixdev")) {
						getLogger().info("Loading: " + clName + " for " + loader);
						
						Class<?> cls = Class.forName(clName, true, loader);
						
						if(cls != null)
							classes.add(cls);
					}
				}
			}
			file.close();
		} catch(IOException | ReflectiveOperationException ex) {
			getLogger().severe("Error ocurred at getting classes, log: " + ex);
			ex.printStackTrace();
		}
		
		return classes;
	}
	
	public static Logger getLogger() {
		return log;
	}
	
	public static Set<Class<?>> loadJar(File jarFile) {
		return loadJar(jarFile, Helius.class.getClassLoader());
	}
	
	public static void main(String[] args) {
		instance = new Helius();
	}
	
	public static SQLTableHandler fastSQLiteConnect(String database, String table) throws NoConnectionException {
		File dbFile = new File(database);
		
		if(dbFile.getParentFile().mkdirs())
			try {
				dbFile.createNewFile();
			} catch(IOException e) {
				throw new NoConnectionException(database);
			}
		
		SQLiteConnector connector = new SQLiteConnector(() -> database);
		
		connector.connect();
		return connector.handler(table);
	}
	
	public static SQLTableHandler fastMySQLConnect(SQLInformation information, String table) throws NoConnectionException {
		MySQLConnector connector = new MySQLConnector(information);
		
		connector.connect();
		return connector.handler(table);
	}
	
	public static File createFile(File file) {
		if(!file.exists() && !file.isDirectory()) {
			try {
				file.getParentFile().mkdirs();
				file.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
	
	public static String read(File file) {
		try {
			return FileUtils.readFileToString(file, Charset.defaultCharset());
		} catch(IOException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static File createFolder(File folder) {
		if(folder.isDirectory())
			folder.mkdirs();
		return folder;
	}
	
	public static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}
	
	public static <V> V runAsync(Callable<V> task) {
		ExecutorService service = Executors.newFixedThreadPool(1);
		CompletionService<V> completionService = new ExecutorCompletionService<>(service);
		completionService.submit(task);
		
		try {
			V val = completionService.take().get();
			
			service.shutdown();
			
			return val;
		} catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
			service.shutdown();
			return null;
		}
	}
	
	public static String getHomePath() {
		return HOME_PATH;
	}
	
	public static HeliusCipher getCipher() {
		return CIPHER;
	}
	
	public static Reflections getReflections() {
		return REFLECTIONS;
	}
	
	public static Helius getInstance() {
		return instance;
	}
	
	public static void addCache(Cache cache) {
		CACHES.add(cache);
	}
}