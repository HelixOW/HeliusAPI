package io.github.alphahelixdev.helius.file.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class YamlFile extends FileConfig {
	
	private static final int YAML_INDENT_SIZE = 4;
	private static final DumperOptions.FlowStyle YAML_FLOW_STYLE = DumperOptions.FlowStyle.BLOCK;
	private final DumperOptions options = new DumperOptions();
	private final Representer representer = new Representer();
	
	private final Yaml yaml;
	
	public YamlFile() {
		this.getOptions().setIndent(YamlFile.getYamlIndentSize());
		this.getOptions().setDefaultFlowStyle(YamlFile.getYamlFlowStyle());
		
		this.getRepresenter().setDefaultFlowStyle(YamlFile.getYamlFlowStyle());
		
		yaml = new Yaml(new Constructor(), this.getRepresenter(), this.getOptions());
	}
	
	public DumperOptions getOptions() {
		return this.options;
	}
	
	public static YamlFile loadFromFile(File file) {
		if(file == null)
			return new YamlFile();
		
		final YamlFile config = new YamlFile();
		
		try {
			config.load(file);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return config;
	}
	
	public static YamlFile loadFromStream(InputStream stream) {
		if(stream == null)
			return new YamlFile();
		
		final YamlFile config = new YamlFile();
		
		try {
			config.load(stream);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return config;
	}
	
	public static int getYamlIndentSize() {
		return YamlFile.YAML_INDENT_SIZE;
	}
	
	public static DumperOptions.FlowStyle getYamlFlowStyle() {
		return YamlFile.YAML_FLOW_STYLE;
	}
	
	public Representer getRepresenter() {
		return this.representer;
	}
	
	public static YamlFile loadFromFile(String filePath) {
		final File file = new File(filePath);
		
		if(!file.isFile())
			return new YamlFile();
		
		return YamlFile.loadFromFile(file);
	}
	
	public String saveToString() {
		return this.getYaml().dump(getValues());
	}
	
	public void loadFromString(String config) {
		if(config == null)
			return;
		
		Map<?, ?> input = null;
		try {
			input = this.getYaml().load(config);
		} catch(YAMLException | ClassCastException e) {
			e.printStackTrace();
		}
		
		if(input != null)
			this.convertMapsToSections(input, this);
	}
	
	public Yaml getYaml() {
		return this.yaml;
	}
	
	private void convertMapsToSections(Map<?, ?> input, ConfigSection section) {
		for(Map.Entry<?, ?> entry : input.entrySet()) {
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();
			
			if(value instanceof Map)
				this.convertMapsToSections((Map<?, ?>) value, section.createSection(key));
			else
				section.set(key, value);
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), this.getOptions(), this.getRepresenter(), this.getYaml());
	}
	
	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;
		if(!super.equals(o)) return false;
		YamlFile yamlFile = (YamlFile) o;
		return Objects.equals(this.getOptions(), yamlFile.getOptions()) &&
				Objects.equals(this.getRepresenter(), yamlFile.getRepresenter()) &&
				Objects.equals(this.getYaml(), yamlFile.getYaml());
	}
	
	@Override
	public String toString() {
		return "YamlFile{" +
				"                            options=" + this.options +
				",                             representer=" + this.representer +
				",                             yaml=" + this.yaml +
				'}';
	}
}
