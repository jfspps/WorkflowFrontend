package company.services.securityServices;

import company.model.security.ClientUser;

import java.util.Set;

public interface ClientUserService extends BaseService<ClientUser, Long> {
    ClientUser findByClientUserName(String username);

    /**
     * Lists all users by the personal name (not username)
     * @param userName personal name
     * @return
     */
    Set<ClientUser> findAllByClientUserName(String userName);
}
