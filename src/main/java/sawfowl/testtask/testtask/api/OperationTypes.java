package sawfowl.testtask.testtask.api;

import java.util.stream.Stream;

public enum OperationTypes {

	DEPOSIT,
	WITHDRAW,
	CREATE,
	DELETE;

	public static OperationTypes fromString(String string) {
		return Stream.of(values()).filter(type -> type.toString().equalsIgnoreCase(string)).findFirst().orElseThrow(
			() -> new RuntimeException(
				"Не верный тип операции `" + string + "`. Допустимые значения: " + String.join(
					", ",
					Stream.of(values()).map(OperationTypes::toString).toList()
				)
			)
		);
	}

}
