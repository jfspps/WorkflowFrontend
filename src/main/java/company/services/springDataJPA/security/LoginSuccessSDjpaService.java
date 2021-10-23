package company.services.springDataJPA.security;

import company.model.security.LoginSuccess;
import company.repositories.security.LoginSuccessRepository;
import company.services.securityServices.LoginSuccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Profile({"H2"})
public class LoginSuccessSDjpaService implements LoginSuccessService {

    private final LoginSuccessRepository loginSuccessRepository;

    public LoginSuccessSDjpaService(LoginSuccessRepository loginSuccessRepository) {
        this.loginSuccessRepository = loginSuccessRepository;
    }

    @Override
    public LoginSuccess save(LoginSuccess object) {
        return loginSuccessRepository.save(object);
    }

    @Override
    public LoginSuccess findById(Long aLong) {
        return loginSuccessRepository.findById(aLong).orElse(null);
    }

    @Override
    public Set<LoginSuccess> findAll() {
        Set<LoginSuccess> loginSuccesses = new HashSet<>();
        loginSuccesses.addAll(loginSuccessRepository.findAll());
        return loginSuccesses;
    }

    @Override
    public void delete(LoginSuccess objectT) {
        loginSuccessRepository.delete(objectT);
    }

    @Override
    public void deleteById(Long aLong) {
        loginSuccessRepository.deleteById(aLong);
    }
}
