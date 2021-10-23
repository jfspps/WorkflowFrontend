package company.services.springDataJPA.security;

import company.model.security.Role;
import company.repositories.security.RoleRepository;
import company.services.securityServices.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Profile({"H2"})
public class RoleSDjpaService implements RoleService {

    private final RoleRepository roleRepository;

    public RoleSDjpaService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role save(Role object) {
        return roleRepository.save(object);
    }

    @Override
    public Role findById(Long aLong) {
        return roleRepository.findById(aLong).orElse(null);
    }

    @Override
    public Set<Role> findAll() {
        Set<Role> roles = new HashSet<>();
        roles.addAll(roleRepository.findAll());
        return roles;
    }

    @Override
    public Role findByRoleName(String roleName) {
        return roleRepository.findByRoleName(roleName).orElse(null);
    }

    @Override
    public void delete(Role objectT) {
        roleRepository.delete(objectT);
    }

    @Override
    public void deleteById(Long aLong) {
        roleRepository.deleteById(aLong);
    }
}
