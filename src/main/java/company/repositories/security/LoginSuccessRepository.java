package company.repositories.security;

import company.model.security.LoginSuccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSuccessRepository extends JpaRepository<LoginSuccess, Long> {
}
