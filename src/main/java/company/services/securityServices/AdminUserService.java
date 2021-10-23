package company.services.securityServices;

import company.model.security.AdminUser;

import java.util.Set;

public interface AdminUserService extends BaseService<AdminUser, Long> {
    AdminUser findByAdminUserName(String username);

    /**
     * Lists all users by the personal name (not username)
     * @param userName personal name
     * @return
     */
    Set<AdminUser> findAllByAdminUserName(String userName);
}
