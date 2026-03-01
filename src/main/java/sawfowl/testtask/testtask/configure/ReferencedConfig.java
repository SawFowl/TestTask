package sawfowl.testtask.testtask.configure;

import java.nio.file.Path;
import java.util.Objects;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

/**
 * Не входило в ТЗ. Добавил для себя на будущее.
 * Текущий проект использую как заготовку для чего-нибудь иного.
 */
public class ReferencedConfig<T, N extends ConfigurationNode> extends SimpleConfig {

	private ConfigurationReference<N> configurationReference;
	private ValueReference<T, N> valueReference;
	private Class<T> clazz;

	public ReferencedConfig(Path configDir, String name, ConfigTypes configType, TypeSerializerCollection serializers, Class<T> clazz) {
		super(configDir, name, configType, serializers);
		Objects.requireNonNull(clazz);
		this.clazz = clazz;
		load();
		if(!getPath().toFile().exists()) save();
	}

	@SuppressWarnings("unchecked")
	public ReferencedConfig(Path configDir, String name, ConfigTypes configType, TypeSerializerCollection serializers, T object) {
		super(configDir, name, configType, serializers);
		Objects.requireNonNull(clazz);
		this.clazz = (Class<T>) object.getClass();
		load();
		if(!getPath().toFile().exists()) save(object);
	}

	public ConfigurationReference<N> getReference() {
		return configurationReference;
	}

	public ValueReference<T, N> getValueReference() {
		return valueReference;
	}

	@SuppressWarnings({ "unchecked", "hiding" })
	@Override
	public <N extends ConfigurationNode, L extends ConfigurationLoader<N>> L getLoader() {
		return (L) getReference().loader();
	}

	public T get() {
		return getValueReference().get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public N getRootNode() {
		return getValueReference().node();
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <C extends SimpleConfig> C load() {
		try {
			configurationReference = (ConfigurationReference<N>) super.selectBuilder(getType()).build().loadToReference();
			configurationReference.load();
			valueReference = configurationReference.referenceTo(clazz);
		} catch (ConfigurateException e) {
			e.printStackTrace();
		}
		return (C) this;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public <C extends SimpleConfig> C save() {
		valueReference.setAndSave(get());
		return (C) this;
	}

	public <E extends T> void save(E object) {
		valueReference.setAndSave(object);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addSerializers(TypeSerializerCollection collection) {
		try {
			configurationReference = (ConfigurationReference<N>) selectBuilder(getType()).path(getPath()).defaultOptions(options -> options.serializers(serializers -> serializers.registerAll(configurationReference.loader().defaultOptions().serializers()).registerAll(collection))).build().loadToReference();
			configurationReference.load();
			valueReference = configurationReference.referenceTo(clazz);
		} catch (ConfigurateException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "unchecked", "hiding" })
	@Override
	public <T, N extends ConfigurationNode, O extends ReferencedConfig<T, N>> O toReference(T config) {
		return (O) this;
	}

	@SuppressWarnings({ "unchecked", "hiding" })
	@Override
	public <T, N extends ConfigurationNode, O extends ReferencedConfig<T, N>> O toReference(Class<T> config) {
		return (O) this;
	}

	@SuppressWarnings({ "unchecked", "hiding" })
	@Override
	public <T, N extends ConfigurationNode, O extends ReferencedConfig<T, N>> O toReference() {
		return (O) this;
	}

	@Override
	public boolean hasReferenced() {
		return true;
	}

}
