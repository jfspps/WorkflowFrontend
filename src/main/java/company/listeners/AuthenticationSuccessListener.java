package company.listeners;

import company.model.security.LoginSuccess;
import company.model.security.User;
import company.repositories.security.LoginSuccessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class AuthenticationSuccessListener {

    private final LoginSuccessRepository loginSuccessRepository;

    @EventListener
    public void listen(AuthenticationSuccessEvent successEvent){
        log.debug("Authentication successful");
        LoginSuccess.LoginSuccessBuilder loginSuccessBuilder = LoginSuccess.builder();

        // check the type of the successEvent before casting, and then extract properties
        if (successEvent.getSource() instanceof UsernamePasswordAuthenticationToken){
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) successEvent.getSource();

            // successEvent.source holds principal and some credentials
            if (token.getPrincipal() instanceof User){
                User user = (User) token.getPrincipal();
                loginSuccessBuilder.user(user);
                log.debug("Username: " + user.getUsername() + " logged in");
            }

            if (token.getDetails() instanceof WebAuthenticationDetails){
                WebAuthenticationDetails details = (WebAuthenticationDetails) token.getDetails();
                loginSuccessBuilder.sourceIP(details.getRemoteAddress());
                log.debug("User IP: " + details.getRemoteAddress());
            }
        }

        LoginSuccess saved = loginSuccessRepository.save(loginSuccessBuilder.build());
        log.debug("Login success record saved, login record ID: " + saved.getId());
    }
}
