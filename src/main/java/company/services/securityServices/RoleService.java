package company.services.securityServices;

import company.model.security.Role;

public interface RoleService extends BaseService<Role, Long> {
    // declare custom (map-related) query methods here
    Role findByRoleName(String roleName);
}
