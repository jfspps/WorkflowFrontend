package company.services.springDataJPA.security;

import company.model.security.ClientUser;
import company.repositories.security.ClientUserRepository;
import company.services.securityServices.ClientUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Profile({"H2"})
public class ClientUserSDjpaService implements ClientUserService {

    private final ClientUserRepository clientUserRepository;

    public ClientUserSDjpaService(ClientUserRepository clientUserRepository) {
        this.clientUserRepository = clientUserRepository;
    }

    @Override
    public ClientUser save(ClientUser object) {
        return clientUserRepository.save(object);
    }

    @Override
    public ClientUser findById(Long aLong) {
        return clientUserRepository.findById(aLong).orElse(null);
    }

    @Override
    public ClientUser findByClientUserName(String username) {
        return clientUserRepository.findByClientUserName(username).orElse(null);
    }

    @Override
    public Set<ClientUser> findAllByClientUserName(String userName) {
        return clientUserRepository.findAllByClientUserName(userName);
    }

    @Override
    public Set<ClientUser> findAll() {
        Set<ClientUser> adminUsers = new HashSet<>();
        adminUsers.addAll(clientUserRepository.findAll());
        return adminUsers;
    }

    @Override
    public void delete(ClientUser objectT) {
        clientUserRepository.delete(objectT);
    }

    @Override
    public void deleteById(Long aLong) {
        clientUserRepository.deleteById(aLong);
    }
}
