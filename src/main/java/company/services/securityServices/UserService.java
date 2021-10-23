package company.services.securityServices;

import company.model.security.User;

import java.util.Set;

public interface UserService extends BaseService<User, Long> {
    // declare custom (map-related) query methods here
    User findByUsername(String username);

    Set<User> findAllByUsername(String username);
}
