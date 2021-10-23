package company.services.springDataJPA.security;

import company.repositories.security.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

//this class could effectively replace the inMemoryAuthentication() class, see SecurityConfiguration configure()

@RequiredArgsConstructor
@Service
@Profile({"H2"})
@Slf4j
public class JPAUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // returns a Spring Security User (as opposed to the custom model.security.User) transferring the custom properties
    // to the Spring Security User properties
    // loadUserByUsername is treated as one transaction with in context getAuthorities in User (otherwise convertToSpringAuthorities()
    // wouldn't find authorities and the user cannot login), particularly when this class substitutes
    // SecurityConfiguration's configure() (note, any WebMvcTests are likely to fail when swapping services, since
    // JPA tests are not part of WebMvcTests)
    @Transactional
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

//            log.debug("Found user: " + username + " with JPAUserDetailsService"); //see /listeners
            return userRepository.findByUsername(username).orElseThrow(() ->
                    new UsernameNotFoundException("User name: " + username + " not found"));
    }
}
