package company.repositories.security;

import company.model.security.LoginFailure;
import company.model.security.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface LoginFailureRepository extends JpaRepository<LoginFailure, Long> {

    //handle lockout
    List<LoginFailure> findAllByUserAndCreatedDateIsAfter(User user, Timestamp timestamp);
}
