package company.services.securityServices;

import company.model.security.RootUser;

public interface RootUserService extends BaseService<RootUser, Long> {
    RootUser findByRootUserName(String username);
}
