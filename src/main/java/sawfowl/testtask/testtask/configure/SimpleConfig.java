package sawfowl.testtask.testtask.configure;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.jackson.FieldValueSeparatorStyle;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.NodeResolver;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.xml.XmlConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import io.leangen.geantyref.TypeToken;

/**
 * Не входило в ТЗ. Добавил для себя на будущее.
 * Текущий проект использую как заготовку для чего-нибудь иного.
 */
public class SimpleConfig {

	private final ConfigTypes type;
	private ConfigurationNode node;
	private ConfigurationLoader<? extends ConfigurationNode> loader;
	private ReferencedConfig<?, ?> referenced;
	private Path path;
	private String name;
	private TypeSerializerCollection serializers;
	private ObjectMapper.Factory factory = ObjectMapper.factoryBuilder().addNodeResolver(NodeResolver.onlyWithSetting()).build();
	public SimpleConfig(Path configDir, String name, ConfigTypes configType, TypeSerializerCollection serializers) {
		this.path = configDir.resolve(name + configType.toString());
		this.type = configType;
		this.name = name;
		if(serializers != null) this.serializers = serializers.childBuilder().registerAnnotatedObjects(factory).build();
		if(!(this instanceof ReferencedConfig)) load();
	}

	public Path getPath() {
		return path;
	}

	public ConfigTypes getType() {
		return type;
	}

	@SuppressWarnings("unchecked")
	public <N extends ConfigurationNode> N getRootNode() {
		return (N) node;
	}

	@SuppressWarnings("unchecked")
	public <N extends ConfigurationNode, L extends ConfigurationLoader<N>> L getLoader() {
		return (L) loader;
	}

	@SuppressWarnings("unchecked")
	public <T, N extends ConfigurationNode, O extends ReferencedConfig<T, N>> O toReference(T config) {
		Objects.requireNonNull(config);
		return (O) (referenced == null ? (referenced = new ReferencedConfig<T, N>(path, name, type, serializers, config)) : referenced);
	}

	@SuppressWarnings("unchecked")
	public <T, N extends ConfigurationNode, O extends ReferencedConfig<T, N>> O toReference(Class<T> config) {
		Objects.requireNonNull(config);
		return (O) (referenced == null ? (referenced = new ReferencedConfig<T, N>(path, name, type, serializers, config)) : referenced);
	}

	@SuppressWarnings("unchecked")
	public <T, N extends ConfigurationNode, O extends ReferencedConfig<T, N>> O toReference() {
		return (O) referenced;
	}

	public <T> boolean addIfNotExist(T object, @Nullable String comment, TypeToken<T> token, Object... path) {
		Objects.requireNonNull(object);
		Objects.requireNonNull(token);
		Objects.requireNonNull(path);
		if(getRootNode().node(path).virtual()) {
			try {
				getRootNode().node(path).set(token, object);
				if(comment != null && getRootNode() instanceof CommentedConfigurationNode commented) commented.node(path).comment(comment);
				return true;
			} catch (SerializationException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public <T> boolean addIfNotExist(T object, @Nullable String comment, Object... path) {
		Objects.requireNonNull(object);
		Objects.requireNonNull(path);
		if(getRootNode().node(path).virtual()) {
			try {
				getRootNode().node(path).set(object.getClass(), object);
				if(comment != null && getRootNode() instanceof CommentedConfigurationNode commented) commented.node(path).comment(comment);
				return true;
			} catch (SerializationException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public <T> boolean addIfNotExist(List<T> object, @Nullable String comment, TypeToken<T> token, Object... path) {
		Objects.requireNonNull(object);
		Objects.requireNonNull(token);
		Objects.requireNonNull(path);
		if(getRootNode().node(path).virtual()) {
			try {
				getRootNode().node(path).setList(token, object);
				if(comment != null && getRootNode() instanceof CommentedConfigurationNode commented) commented.node(path).comment(comment);
				return true;
			} catch (SerializationException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public <T> boolean addIfNotExist(Class<T> clazz, List<T> object, @Nullable String comment, Object... path) {
		Objects.requireNonNull(object);
		Objects.requireNonNull(clazz);
		Objects.requireNonNull(path);
		if(getRootNode().node(path).virtual()) {
			try {
				getRootNode().node(path).setList(clazz, object);
				if(comment != null && getRootNode() instanceof CommentedConfigurationNode commented) commented.node(path).comment(comment);
				return true;
			} catch (SerializationException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void addSerializers(TypeSerializerCollection collection) {
		try {
			loader = selectBuilder(type).path(path).defaultOptions(options -> options.serializers(serializers -> serializers.registerAll(loader.defaultOptions().serializers()).registerAll(collection))).build();
			node = loader.load();
		} catch (ConfigurateException e) {
			e.printStackTrace();
		}
	}

	public boolean fileExist() {
		return path.toFile().exists();
	}

	public boolean hasReferenced() {
		return referenced != null;
	}

	@SuppressWarnings("unchecked")
	public <C extends SimpleConfig> C load() {
		try {
			if(loader == null) loader = selectBuilder(type).path(path).build();
			node = loader.load();
		} catch (ConfigurateException e) {
			e.printStackTrace();
		}
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	public <C extends SimpleConfig> C save() {
		try {
			loader.save(node);
		} catch (ConfigurateException e) {
			e.printStackTrace();
		}
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	<B extends AbstractConfigurationLoader.Builder<B, ?>> B selectBuilder(ConfigTypes loaderType) {
		if(serializers != null) {
			switch (loaderType) {
				case YAML: return (B) YamlConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAll(serializers))).nodeStyle(NodeStyle.BLOCK).path(path);
				case XML: return (B) XmlConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAll(serializers))).writesExplicitType(true);
				case JSON: return (B) GsonConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAll(serializers))).path(path);
				case JACKSON: return (B) JacksonConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAll(serializers))).fieldValueSeparatorStyle(FieldValueSeparatorStyle.SPACE_BOTH_SIDES);
				default: return (B) HoconConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAll(serializers))).path(path);
			}
		}
		switch (loaderType) {
			case YAML: return (B) YamlConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAnnotatedObjects(factory))).nodeStyle(NodeStyle.BLOCK).path(path);
			case XML: return (B) XmlConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAnnotatedObjects(factory))).writesExplicitType(true);
			case JSON: return (B) GsonConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAnnotatedObjects(factory))).path(path);
			case JACKSON: return (B) JacksonConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAnnotatedObjects(factory))).fieldValueSeparatorStyle(FieldValueSeparatorStyle.SPACE_BOTH_SIDES);
			default: return (B) HoconConfigurationLoader.builder().defaultOptions(options -> options.serializers(s -> s.registerAnnotatedObjects(factory))).path(path);
		}
	}

	protected String getName() {
		return name;
	}

}
