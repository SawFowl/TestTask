package sawfowl.testtask.testtask.api;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sawfowl.testtask.testtask.TesttaskApplication;

@RestController
@RequestMapping
public class WalletsController {

	private final WalletService walletService;
	private JdbcTemplate h2Jdbc;

	public WalletsController(WalletService walletService) {
		this.walletService = walletService;
		putDefaultData();
	}

	public WalletService getWalletService() {
		return walletService;
	}

	@GetMapping({"/", "/api", "/api/", "/api/v1", "/api/v1/"})
	public String messageAPI() {
		return 
			"Используйте json POST запрос на `/api/v1/wallet` или укажите UUID кошелька на `/api/v1/wallets/{uuid}` для отображения информации по нему.<br>"
			+ "Для вывода списка всех данных в виде текста перейдите по адресу `/api/v1/printall`.<br>"
			+ "Для вывода списка всех данных в виде JSON перейдите по адресу `/api/v1/allwallets`.<br>"
			+ "Для миграции данных в БД H2 перейдите по адресу `/api/v1/migration/h2backup`.<br>"
			+ "Для загрузки бэкапа из БД H2 перейдите по адресу `/api/v1/migration/h2load`.<br><br>"
			+ "Пример POST запроса через curl для создания записи: `<b>curl -X POST http://localhost:8080/api/v1/wallet   -H \"Content-Type: application/json\"   -d '{\"valletId\":\"123e4567-e89b-12d3-a456-426614174000\",\"operationType\":\"CREATE\",\"amount\":200}'</b>`<br>"
			+ "Пример POST запроса через curl для удаления записи: `<b>curl -X POST http://localhost:8080/api/v1/wallet   -H \"Content-Type: application/json\"   -d '{\"valletId\":\"123e4567-e89b-12d3-a456-426614174000\",\"operationType\":\"DELETE\"}'</b>`<br>"
			+ "Пример POST запроса через curl для увеличения баланса кошелька: `<b>curl -X POST http://localhost:8080/api/v1/wallet   -H \"Content-Type: application/json\"   -d '{\"valletId\":\"123e4567-e89b-12d3-a456-426614174000\",\"operationType\":\"DEPOSIT\",\"amount\":200}'</b>`<br>"
			+ "Пример POST запроса через curl для уменьшения баланса кошелька: `<b>curl -X POST http://localhost:8080/api/v1/wallet   -H \"Content-Type: application/json\"   -d '{\"valletId\":\"123e4567-e89b-12d3-a456-426614174000\",\"operationType\":\"WITHDRAW\",\"amount\":200}'</b>`<br>"
		;
	}

	@GetMapping("/api/v1/printall")
	public String printAll() {
		return "Всего записей в БД " + walletService.total() + ":<br>" + String.join("<br>", walletService.getAll().stream().map(w -> "UUID: " + w.getUuid().toString() + " | Баланс: " + w.getBalance()).toList());
	}

	@PostMapping("/api/v1/wallet")
	public ResponseEntity<?> processRawBody(@RequestBody String rawBody) {
		//System.out.println("Полученный запрос: " + rawBody);
		if(rawBody.isEmpty()) return ResponseEntity.badRequest().build();
		Wallet wallet = null;
		try {
			JsonElement json = JsonParser.parseString(rawBody);
			if(json instanceof JsonObject object) {
				if(!object.has("valletId")) return ResponseEntity.badRequest().body("Отсутствует id кошелька. Поле в JSON -> `valletId`.");
				if(!object.has("operationType")) return ResponseEntity.badRequest().body("Не указан тип операции. Поле в JSON -> `operationType`.");
				UUID uuid = null;
				OperationTypes operationType = null;
				try {
					uuid = UUID.fromString(object.get("valletId").getAsString());
				} catch (Exception e) {
					return ResponseEntity.badRequest().body("Ошибка парсинга UUID: " + e.getMessage());
				}
				try {
					operationType = OperationTypes.fromString(object.get("operationType").getAsString());
				} catch (Exception e) {
					return ResponseEntity.badRequest().body("Ошибка парсинга типа операции: " + e.getMessage());
				}
				switch (operationType) {
					case CREATE: {
						if(walletService.exist(uuid)) return ResponseEntity.badRequest().body("Кошелек с таким UUID(" + object.get("valletId").getAsString() + " ) уже существует.");
						wallet = walletService.create(uuid, !object.has("amount") || !object.get("amount").isJsonPrimitive() || !object.get("amount").getAsJsonPrimitive().isNumber() ? 0 : object.get("amount").getAsJsonPrimitive().getAsLong());
						return ResponseEntity.created(URI.create("/api/v1/wallets/" + object.get("valletId").getAsString())).body(wallet);
					}
					case DELETE: {
						if(!walletService.exist(uuid)) return ResponseEntity.badRequest().body("Кошелек с таким UUID(" + object.get("valletId").getAsString() + " ) не существует.");
						walletService.delete(uuid);
						return ResponseEntity.noContent().build();
					}
					case DEPOSIT: {
						if(!walletService.exist(uuid)) return ResponseEntity.badRequest().body("Кошелек с таким UUID(" + object.get("valletId").getAsString() + " ) не существует.");
						if(!object.has("amount") || !object.get("amount").isJsonPrimitive() || !object.get("amount").getAsJsonPrimitive().isNumber())
							return ResponseEntity.badRequest().body("Не указан объем. Поле в JSON -> `amount`. Значение должно быть числом.");
						long amount = object.get("amount").getAsJsonPrimitive().getAsLong();
						if(amount < 0) return ResponseEntity.badRequest().body("Значение `amount` должно быть больше 0.");
						wallet = walletService.getOrThrow(uuid);
						wallet.deposit(object.get("amount").getAsJsonPrimitive().getAsLong());
						walletService.save(wallet);
						break;
					}
					case WITHDRAW: {
						if(!walletService.exist(uuid)) return ResponseEntity.badRequest().body("Кошелек с таким UUID(" + object.get("valletId").getAsString() + " ) не существует.");
						if(!object.has("amount") || !object.get("amount").isJsonPrimitive() || !object.get("amount").getAsJsonPrimitive().isNumber())
							return ResponseEntity.badRequest().body("Не указан объем. Поле в JSON -> `amount`. Значение должно быть числом.");
						long amount = object.get("amount").getAsJsonPrimitive().getAsLong();
						if(amount < 0) return ResponseEntity.badRequest().body("Значение `amount` должно быть больше 0.");
						wallet = walletService.getOrThrow(uuid);
						if(wallet.getBalance() < amount) return ResponseEntity.badRequest().body("На балансе не достаточно средств.");
						wallet.withdraw(amount);
						walletService.save(wallet);
						break;
					}
				}
			} else return ResponseEntity.badRequest().body("Не подходящий JSON: `" + rawBody + "`. Нужен JSON с картой данных.");
			return ResponseEntity.ok().body(wallet);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("Ошибка парсинга JSON: " + e.getMessage());
		}
	}

	@GetMapping("/api/v1/allwallets")
	public ResponseEntity<List<Wallet>> getByUUID() {
		return ResponseEntity.ok(walletService.getAll());
	}

	@GetMapping("/api/v1/wallets/{uuid}")
	public ResponseEntity<Wallet> getByUUID(@PathVariable("uuid") UUID uuid) {
		Optional<Wallet> wallet = walletService.findByUUID(uuid);
		if (wallet.isEmpty()) {
			throw new WalletNotFoundException("Кошелек не найден: " + uuid);
		}
		return ResponseEntity.ok(wallet.get());
	}

	@DeleteMapping("/api/v1/wallets/delete/{uuid}")
	public ResponseEntity<Void> delete(@PathVariable("uuid") UUID uuid) {
		walletService.delete(uuid);
		return ResponseEntity.noContent().build();
	}

	public void putDefaultData() {
		if(walletService.total() > 0) return;
		walletService.create(UUID.randomUUID(), 0);
		walletService.create(UUID.randomUUID(), 1234);
		walletService.create(UUID.randomUUID(), 5423);
		walletService.create(UUID.randomUUID(), 723);
		walletService.create(UUID.randomUUID(), 123);
		walletService.create(UUID.randomUUID(), 341);
		walletService.create(UUID.randomUUID(), 1324);
		walletService.create(UUID.randomUUID(), 1);
		walletService.create(UUID.randomUUID(), 5123);
		walletService.create(UUID.randomUUID(), 40);
	}

	@GetMapping("/api/v1/migration/h2backup")
	public String migrationSaveBackup() {
		if(h2Jdbc == null) h2Jdbc = new JdbcTemplate(createH2DataSource());
		String createTable = "CREATE TABLE IF NOT EXISTS wallets(\n"
				+ "    uuid UUID DEFAULT RANDOM_UUID() PRIMARY KEY,\n"
				+ "    balance BIGINT NOT NULL DEFAULT 0\n"
				+ ");";
		h2Jdbc.execute(createTable);
		h2Jdbc.execute("TRUNCATE TABLE wallets");
		h2Jdbc.batchUpdate(
			"MERGE INTO wallets (uuid, balance) VALUES (?, ?)",
			walletService.getAll(),
			100,
			(ps, wallet) -> {
				ps.setObject(1, wallet.getUuid());
				ps.setLong(2, wallet.getBalance());
			}
		);
		String result = "";
		try(Statement statement = h2Jdbc.getDataSource().getConnection().createStatement()) {
			ResultSet resultSet = statement.executeQuery("SELECT * FROM wallets");
			while(resultSet.next()) {
				System.out.println(resultSet.getString("uuid") + "  " + resultSet.getLong("balance"));
				result += resultSet.getString("uuid") + " -> " + resultSet.getLong("balance") + "<br>";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "Выполнена миграция в базу данных h2. Записи в БД h2:<br>" + result;
	}

	@GetMapping("/api/v1/migration/h2load")
	public String migrationLoadBackup() {
		if(!TesttaskApplication.getRootPath().resolve("BackupH2.mv.db").toFile().exists()) return "Отсутствует файл БД h2. Нечего загружать.";
		if(h2Jdbc == null) h2Jdbc = new JdbcTemplate(createH2DataSource());
		List<Wallet> wallets = new ArrayList<>();
		try (Statement statement = h2Jdbc.getDataSource().getConnection().createStatement()) {
			ResultSet resultSet = statement.executeQuery("SELECT * FROM wallets");
			while(resultSet.next()) wallets.add(new Wallet(UUID.fromString(resultSet.getString("uuid")), resultSet.getLong("balance")));
			if(wallets.isEmpty()) return "База данных пуста. Нечего загружать.";
			walletService.saveAll(wallets);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "Выполнена загрузка данных из БД h2";
	}

	private DataSource createH2DataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("org.h2.Driver");
		dataSource.setUrl("jdbc:h2:" + TesttaskApplication.getRootPath().resolve("BackupH2").toFile().getAbsolutePath() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
		dataSource.setUsername("user");
		dataSource.setPassword("");
		String createTable = "CREATE TABLE IF NOT EXISTS wallets(\n"
				+ "    uuid UUID DEFAULT RANDOM_UUID() PRIMARY KEY,\n"
				+ "    balance BIGINT NOT NULL DEFAULT 0\n"
				+ ");";
		try {
			dataSource.getConnection().createStatement().execute(createTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return dataSource;
	}

}
