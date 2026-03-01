package sawfowl.testtask.testtask.api;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallets")
public class Wallet {

	public Wallet() {}

	public Wallet(UUID uuid, long balance) {
		this.uuid = uuid;
		this.balance = balance;
	}

	@Id
	@Column(name = "uuid")
	private UUID uuid;
	@Column(name = "balance")
	private long balance;

	public UUID getUuid() {
		return uuid;
	}

	public long getBalance() {
		return balance;
	}

	public synchronized void setBalance(long balance) {
		this.balance = balance;
	}

	public synchronized void deposit(long amount) {
		balance += amount;
	}

	public synchronized void withdraw(long amount) {
		balance -= amount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		return Objects.equals(uuid, ((Wallet) obj).uuid);
	}

	@Override
	public String toString() {
		return "Wallet [uuid=" + uuid + ", balance=" + balance + "]";
	}

}
