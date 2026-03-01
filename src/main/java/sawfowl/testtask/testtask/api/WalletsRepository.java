package sawfowl.testtask.testtask.api;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletsRepository extends JpaRepository<Wallet, UUID> {

}
