package sawfowl.testtask.testtask.configure;

import java.util.stream.Stream;

/**
 * Не входило в ТЗ. Добавил для себя на будущее.
 * Текущий проект использую как заготовку для чего-нибудь иного.
 */
public enum ConfigTypes {

	HOCON(".conf") {
		@Override
		public String toString() {
			return ".conf";
		}
		@Override
		public String getExtension() {
			return "conf";
		}
		@Override
		public String getTypeName() {
			return "Hocon";
		}
	},
	JSON(".json") {
		@Override
		public String toString() {
			return ".json";
		}
		@Override
		public String getExtension() {
			return "json";
		}
		@Override
		public String getTypeName() {
			return "Json";
		}
	},
	YAML(".yml") {
		@Override
		public String toString() {
			return ".yml";
		}
		@Override
		public String getExtension() {
			return "yml";
		}
		@Override
		public String getTypeName() {
			return "Yaml";
		}
	},
	XML(".xml") {
		@Override
		public String toString() {
			return ".properties";
		}
		@Override
		public String getExtension() {
			return "properties";
		}
		@Override
		public String getTypeName() {
			return "XML";
		}
	},
	JACKSON(".json") {
		@Override
		public String toString() {
			return ".json";
		}
		@Override
		public String getExtension() {
			return "json";
		}
		@Override
		public String getTypeName() {
			return "Jackson";
		}
	},
	UNKNOWN("") {
		@Override
		public String getTypeName() {
			return "UNKNOWN";
		}
	};

	ConfigTypes(String string) {}

	public String getExtension() {
		return "";
	}

	public abstract String getTypeName();

	public static ConfigTypes getByType(String type) {
		return Stream.of(ConfigTypes.values()).filter(value -> value.getTypeName().equalsIgnoreCase(type)).findFirst().orElse(UNKNOWN);
	}

	public static boolean isValidExtension(String extension) {
		return !extension.isEmpty() && Stream.of(ConfigTypes.values()).filter(v -> v.getExtension().equals(extension)).findFirst().isPresent();
	}

	public static ConfigTypes getTypeByExtension(String extension) {
		return Stream.of(ConfigTypes.values()).filter(v -> v.toString().equals(extension) || v.getExtension().equals(extension)).findFirst().orElse(HOCON);
	}

}
