package company.repositories.security;

import company.model.security.AdminUser;
import company.model.security.RootUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RootUserRepository extends JpaRepository<RootUser, Long> {
    Optional<RootUser> findByRootUserName(String username);
}
