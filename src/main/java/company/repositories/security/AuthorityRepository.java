package company.repositories.security;

import company.model.security.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {
    // add custom JPA queries here
}
