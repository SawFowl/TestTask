package sawfowl.testtask.testtask.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppExceptionHandler {

	@ExceptionHandler(WalletNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
		Map<String, Object> response = new HashMap<>();
		response.put("message", ex.getMessage());
		response.put("status", HttpStatus.NOT_FOUND.value());
		response.put("timestamp", System.currentTimeMillis());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
	}

}
