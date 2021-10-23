package company.services.springDataJPA.security;

import company.model.security.Authority;
import company.repositories.security.AuthorityRepository;
import company.services.securityServices.AuthorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Profile({"H2"})
public class AuthoritySDjpaService implements AuthorityService {

    private final AuthorityRepository authorityRepository;

    public AuthoritySDjpaService(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    @Override
    public Authority save(Authority object) {
        return authorityRepository.save(object);
    }

    @Override
    public Authority findById(Long aLong) {
        return authorityRepository.findById(aLong).orElse(null);
    }

    @Override
    public Set<Authority> findAll() {
        Set<Authority> authorities = new HashSet<>();
        authorities.addAll(authorityRepository.findAll());
        return authorities;
    }

    @Override
    public void delete(Authority objectT) {
        authorityRepository.delete(objectT);
    }

    @Override
    public void deleteById(Long aLong) {
        authorityRepository.deleteById(aLong);
    }
}
