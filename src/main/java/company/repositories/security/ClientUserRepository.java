package company.repositories.security;

import company.model.security.ClientUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface ClientUserRepository extends JpaRepository<ClientUser, Long> {
    Optional<ClientUser> findByClientUserName(String username);

    Set<ClientUser> findAllByClientUserName(String userName);
}
