package sawfowl.testtask.testtask;

import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TesttaskApplication {

	private static final Path ROOT_PATH = Path.of("", new String[0]);

	public static void main(String[] args) {
		SpringApplication.run(TesttaskApplication.class, args);
	}

	public static Path getRootPath() {
		return ROOT_PATH;
	}

}
