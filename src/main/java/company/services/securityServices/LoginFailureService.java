package company.services.securityServices;

import company.model.security.LoginFailure;
import company.model.security.User;

import java.sql.Timestamp;
import java.util.List;

public interface LoginFailureService extends BaseService<LoginFailure, Long> {

    //handle lockout
    List<LoginFailure> findAllByUserAndCreatedDateIsAfter(User user, Timestamp timestamp);
}
