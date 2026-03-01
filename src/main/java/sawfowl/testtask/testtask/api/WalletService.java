package sawfowl.testtask.testtask.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

	private final WalletsRepository walletsRepository;

	@Autowired
	public WalletService(WalletsRepository walletsRepository) {
		this.walletsRepository = walletsRepository;
	}

	@Transactional
	public Wallet create(UUID uuid, long balance) {
		if (walletsRepository.existsById(uuid)) {
			throw new RuntimeException("Кошелек с этим UUID уже существует!");
		}
		Wallet wallet = new Wallet(uuid, balance);
		return walletsRepository.save(wallet);
	}

	public void saveAll(List<Wallet> wallets) {
		walletsRepository.saveAllAndFlush(wallets);
	}

	public void save(Wallet wallet) {
		walletsRepository.saveAndFlush(wallet);
	}

	public long total() {
		return walletsRepository.count();
	}

	public boolean exist(UUID uuid) {
		return walletsRepository.existsById(uuid);
	}

	public Wallet getOrThrow(UUID uuid) {
		return findByUUID(uuid).orElseThrow(() -> new RuntimeException("Кошелек не найден"));
	}

	public Optional<Wallet> findByUUID(UUID uuid) {
		return walletsRepository.findById(uuid);
	}

	public List<Wallet> getAll() {
		return walletsRepository.findAll();
	}

	@Transactional
	public void delete(UUID uuid) {
		walletsRepository.deleteById(uuid);
	}

}
