package company.bootstrap.security;

import company.model.security.*;
import company.services.TestRecordService;
import company.services.securityServices.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("H2")
public class DataLoader implements CommandLineRunner {

    private final AuthorityService authorityService;
    private final RoleService roleService;
    private final AdminUserService adminUserService;
    private final ClientUserService clientUserService;
    private final TestRecordService testRecordService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Override
    public void run(String... args) {
        if (userService.findAll().size() == 0){
            loadSecurityData();
            log.debug("Users database finished populating");
            loadAdminUsers();
            loadClientUsers();
        } else
            log.debug("Users database already contains data; no changes made");

        loadTestRecord();
        log.debug("TestRecords loaded: " + testRecordService.findAll().size());

        log.debug("Users on file: " + userService.findAll().size());
        log.debug("Authorities on file: " + authorityService.findAll().size());
        log.debug("Roles on file: " + roleService.findAll().size());
        log.debug("Finished Dataloader ================================================");
    }

    private void loadTestRecord() {
        testRecordService.createTestRecord("Test record 1", "jamesapps");
        testRecordService.createTestRecord("Test record 2", "paulsmith");
    }

    private void loadSecurityData(){
        // Privileges Root > Admin > Client
        // all permissions below are in relation to TestRecord CRUD ops

        //root authorities
        Authority createRoot = authorityService.save(Authority.builder().permission("root.create").build());
        Authority updateRoot = authorityService.save(Authority.builder().permission("root.update").build());
        Authority readRoot = authorityService.save(Authority.builder().permission("root.read").build());
        Authority deleteRoot = authorityService.save(Authority.builder().permission("root.delete").build());

        //admin authorities
        Authority createAdmin = authorityService.save(Authority.builder().permission("admin.create").build());
        Authority updateAdmin = authorityService.save(Authority.builder().permission("admin.update").build());
        Authority readAdmin = authorityService.save(Authority.builder().permission("admin.read").build());
        Authority deleteAdmin = authorityService.save(Authority.builder().permission("admin.delete").build());

        //client authorities
        Authority createClient = authorityService.save(Authority.builder().permission("client.create").build());
        Authority updateClient = authorityService.save(Authority.builder().permission("client.update").build());
        Authority readClient = authorityService.save(Authority.builder().permission("client.read").build());
        Authority deleteClient = authorityService.save(Authority.builder().permission("client.delete").build());

        Role rootRole = roleService.save(Role.builder().roleName("ROOT").build());
        Role adminRole = roleService.save(Role.builder().roleName("ADMIN").build());
        Role clientRole = roleService.save(Role.builder().roleName("CLIENT").build());

        //Set.Of returns an immutable set, so new HashSet instantiates a mutable Set
        rootRole.setAuthorities(new HashSet<>(Set.of(createRoot, readRoot, updateRoot, deleteRoot,
                createAdmin, updateAdmin, readAdmin, deleteAdmin,
                createClient, readClient, updateClient, deleteClient)));

        adminRole.setAuthorities(new HashSet<>(Set.of(createAdmin, updateAdmin, readAdmin, deleteAdmin,
                createClient, readClient, updateClient, deleteClient)));

        clientRole.setAuthorities(new HashSet<>(Set.of(createClient, readClient, updateClient, deleteClient)));

        roleService.save(rootRole);
        roleService.save(adminRole);
        roleService.save(clientRole);

        log.debug("Roles added: " + roleService.findAll().size());
        log.debug("Authorities added: " + authorityService.findAll().size());
    }

    public void loadAdminUsers(){
        Role adminRole = roleService.findByRoleName("ADMIN");

        // Instantiating the admin users (this must be done after Users)
        // Note, UserName is not Username
        AdminUser theAdmin = adminUserService.save(AdminUser.builder().adminUserName("The Admin").build());

        User theBoss = userService.save(User.builder().username("admin")
                .password(passwordEncoder.encode("admin123"))
                .adminUser(theAdmin)
                .role(adminRole).build());

        log.debug("AdminUsers added: " + theBoss.getUsername());
    }

    public void loadClientUsers(){
        Role clientRole = roleService.findByRoleName("CLIENT");

        // Instantiating the client users (this must be done after Users)
        ClientUser paulSmith = clientUserService.save(ClientUser.builder().clientUserName("Paul Smith").build());

        User paulSmithUser = userService.save(User.builder().username("paulsmith")
                .password(passwordEncoder.encode("paulsmith123"))
                .clientUser(paulSmith)
                .role(clientRole).build());
        //other ClientUsers can be assigned to paulsmith, with the same roles

        log.debug("Client users added: " + paulSmithUser.getUsername());
    }
}