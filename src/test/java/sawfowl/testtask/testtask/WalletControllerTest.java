package sawfowl.testtask.testtask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import sawfowl.testtask.testtask.api.Wallet;
import sawfowl.testtask.testtask.api.WalletService;
import sawfowl.testtask.testtask.api.WalletsController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(WalletsController.class)
class WalletControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@MockitoBean
	private WalletService walletService;
	private UUID testWalletId;
	private Wallet testWallet;

	@BeforeEach
	void setUp() {
		testWalletId = UUID.randomUUID();
		testWallet = new Wallet(testWalletId, 1000);
	}

	// ==================== БАЗОВЫЕ ТЕСТЫ С ОПЕРАЦИЯМИ НАД КОШЕЛЬКАМИ ====================

	@Test
	void getAll() {
		List<Wallet> users = Arrays.asList(
			testWallet,
			new Wallet(UUID.randomUUID(), 2000)
		);
		when(walletService.getAll()).thenReturn(users);
		try {
			mockMvc.perform(get("/api/v1/allwallets")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].uuid", is(testWalletId.toString())))
				.andExpect(jsonPath("$[0].balance", is(1000)))
				.andExpect(jsonPath("$[1].balance", is(2000)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	void walletExist() throws Exception {
		when(walletService.findByUUID(testWalletId)).thenReturn(Optional.of(testWallet));
		mockMvc.perform(get("/api/v1/wallets/{uuid}", testWalletId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid", is(testWalletId.toString())))
				.andExpect(jsonPath("$.balance", is(1000)));
	}

	@Test
	void walletNotFound() throws Exception {
		UUID nonExistentId = UUID.randomUUID();
		when(walletService.findByUUID(nonExistentId)).thenReturn(Optional.empty());
		mockMvc.perform(get("/api/v1/wallets/{uuid}", nonExistentId)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound());
	}

	 @Test
	void create() throws Exception {
		UUID walletId = UUID.randomUUID();
		Wallet expectedWallet = new Wallet(walletId, 1000L);
		String validJson = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"CREATE\", \"amount\": %d}",
			walletId.toString(), 200L
		);
		when(walletService.create(any(), anyLong())).thenReturn(expectedWallet);
		mockMvc.perform(post("/api/v1/wallet")
			.contentType(MediaType.APPLICATION_JSON)
			.content(validJson))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.uuid").value(walletId.toString()))
			.andExpect(jsonPath("$.balance").value(1000));
	}

	@Test
	void deleteExistingWallet() throws Exception {
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DELETE\"}",
			testWalletId
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		doNothing().when(walletService).delete(testWalletId);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isNoContent())  // 204 No Content
				.andExpect(content().string(""));   // пустое тело ответа

		verify(walletService, times(1)).exist(testWalletId);
		verify(walletService, times(1)).delete(testWalletId);
		verify(walletService, never()).save(any(Wallet.class));
		verify(walletService, never()).getOrThrow(any(UUID.class));
	}

	@Test
	void deleteNotExistWallet() throws Exception {
		UUID notExistWallet = UUID.randomUUID();
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DELETE\"}",
			notExistWallet
		);
		when(walletService.exist(notExistWallet)).thenReturn(false);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("Кошелек с таким UUID(" + notExistWallet + " ) не существует")));
		verify(walletService, times(1)).exist(notExistWallet);
		verify(walletService, never()).delete(any(UUID.class));
	}

	@Test
	void createWalletWithInvalidJson() throws Exception {
		String invalidJson = "{\"valletId\":\"plain_string\",\"operationType\":\"CREATE\",\"amount\":\"not-a-number\"}";
		mockMvc.perform(post("/api/v1/wallet")
			.contentType(MediaType.APPLICATION_JSON)
			.content(invalidJson))
			.andExpect(status().isBadRequest());
		verify(walletService, never()).create(any(), anyLong());
	}

	@Test
	void nullValue() throws Exception {
		String jsonWithNull = "{\"valletId\":\"null\",\"operationType\":\"CREATE\",\"amount\":200}"; 
		mockMvc.perform(post("/api/v1/wallet")
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonWithNull))
			.andExpect(status().isBadRequest());
		verify(walletService, never()).create(any(), anyLong());
	}

	@Test
	void empty() throws Exception {
		String emptyBody = "";
		mockMvc.perform(post("/api/v1/wallet")
			.contentType(MediaType.APPLICATION_JSON)
			.content(emptyBody))
			.andExpect(status().isBadRequest());
		verify(walletService, never()).create(any(), anyLong());
	}

	// ==================== ТЕСТЫ ЗАЧИСЛЕНИЯ НА БАЛАНС ====================

	@Test
	void depositValidData() throws Exception {
		Long depositAmount = 500L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\", \"amount\": %d}",
			testWalletId, depositAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		doNothing().when(walletService).save(any(Wallet.class));
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid").value(testWalletId.toString()))
				.andExpect(jsonPath("$.balance").value(1500));
		verify(walletService, times(1)).exist(testWalletId);
		verify(walletService, times(1)).getOrThrow(testWalletId);
		verify(walletService, times(1)).save(testWallet);
	}

	@Test
	void depositWalletNotFound() throws Exception {
		UUID nonExistentId = UUID.randomUUID();
		Long depositAmount = 500L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\", \"amount\": %d}",
			nonExistentId, depositAmount
		);
		when(walletService.exist(nonExistentId)).thenReturn(false);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("не существует")));
		verify(walletService, times(1)).exist(nonExistentId);
		verify(walletService, never()).getOrThrow(any(UUID.class));
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void depositWithoutAmount() throws Exception {
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\"}",
			testWalletId
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("Не указан объем")));
		verify(walletService, times(1)).exist(testWalletId);
		verify(walletService, never()).getOrThrow(any(UUID.class));
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void depositAmountAsString() throws Exception {
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\", \"amount\": \"500\"}",
			testWalletId
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("должно быть числом")));
		verify(walletService, never()).getOrThrow(any(UUID.class));
	}

	@Test
	void depositNegativeAmount() throws Exception {
		Long negativeAmount = -100L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\", \"amount\": %d}",
			testWalletId, negativeAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("больше 0")));
		verify(walletService, never()).getOrThrow(any(UUID.class));
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void depositVeryLargeAmount() throws Exception {
		Long hugeAmount = Long.MAX_VALUE;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\", \"amount\": %d}",
			testWalletId, hugeAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk());
		verify(walletService, times(1)).save(testWallet);
	}

	// ==================== ТЕСТЫ СПИСАНИЯ С БАЛАНСА ====================

	@Test
	void withdrawValidData() throws Exception {
		Long withdrawAmount = 300L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			testWalletId, withdrawAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uuid").value(testWalletId.toString()))
				.andExpect(jsonPath("$.balance").value(700));
		verify(walletService, times(1)).exist(testWalletId);
		verify(walletService, times(1)).getOrThrow(testWalletId);
		verify(walletService, times(1)).save(testWallet);
	}

	@Test
	void withdrawInsufficientFunds() throws Exception {
		Long withdrawAmount = 2000L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			testWalletId, withdrawAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("не достаточно средств")));
		verify(walletService, times(1)).exist(testWalletId);
		verify(walletService, times(1)).getOrThrow(testWalletId);
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void withdrawWalletNotFound() throws Exception {
		UUID nonExistentId = UUID.randomUUID();
		Long withdrawAmount = 300L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			nonExistentId, withdrawAmount
		);
		when(walletService.exist(nonExistentId)).thenReturn(false);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("не существует")));
		verify(walletService, never()).getOrThrow(any(UUID.class));
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void withdrawWithoutAmount() throws Exception {
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\"}",
			testWalletId
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("Не указан объем")));
		verify(walletService, never()).getOrThrow(any(UUID.class));
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void withdrawNegativeAmount() throws Exception {
		Long negativeAmount = -100L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			testWalletId, negativeAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("больше 0")));
		verify(walletService, never()).getOrThrow(any(UUID.class));
		verify(walletService, never()).save(any(Wallet.class));
	}

	@Test
	void withdrawAmountAsString() throws Exception {
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": \"300\"}",
			testWalletId
		);

		when(walletService.exist(testWalletId)).thenReturn(true);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("должно быть числом")));
		verify(walletService, never()).getOrThrow(any(UUID.class));
	}

	@Test
	void withdrawZeroAmount() throws Exception {
		Long zeroAmount = 0L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			testWalletId, zeroAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(1000));
		verify(walletService, times(1)).save(testWallet);
	}

	@Test
	void withdrawExactBalance() throws Exception {
		Long withdrawAmount = 1000L;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			testWalletId, withdrawAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(0));
		verify(walletService, times(1)).save(testWallet);
	}

	@Test
	void withdrawVeryLargeAmount() throws Exception {
		Long hugeAmount = Long.MAX_VALUE;
		String jsonRequest = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": %d}",
			testWalletId, hugeAmount
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(content().string(containsString("не достаточно средств")));
		verify(walletService, never()).save(any(Wallet.class));
	}

	// ==================== ТЕСТ ЗАЧИСЛЕНИЯ И ПОСЛЕДУЮЩЕГО СНЯТИЯ С БАЛАНСА ====================
	@Test
	void multipleOperations() throws Exception {
		String depositJson = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"DEPOSIT\", \"amount\": 500}",
			testWalletId
		);
		when(walletService.exist(testWalletId)).thenReturn(true);
		when(walletService.getOrThrow(testWalletId)).thenReturn(testWallet);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(depositJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(1500));
		String withdrawJson = String.format(
			"{\"valletId\": \"%s\", \"operationType\": \"WITHDRAW\", \"amount\": 300}",
			testWalletId
		);
		mockMvc.perform(post("/api/v1/wallet")
				.contentType(MediaType.APPLICATION_JSON)
				.content(withdrawJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(1200));
		verify(walletService, times(2)).save(testWallet);
	}

}
