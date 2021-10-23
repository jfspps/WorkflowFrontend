package company.web.controllers;

import company.model.security.AdminUser;
import company.model.security.ClientUser;
import company.model.security.Role;
import company.model.security.User;
import company.services.securityServices.AdminUserService;
import company.services.securityServices.ClientUserService;
import company.services.securityServices.RoleService;
import company.services.securityServices.UserService;
import company.web.permissionAnnot.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final AdminUserService adminUserService;
    private final ClientUserService clientUserService;
    private final PasswordEncoder passwordEncoder;

    public static final String INVALID_PASSWORD = "Password length must be >= 8 characters";

    //prevent the HTTP form POST from editing listed properties
    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @GetMapping({"/", "/welcome"})
    public String welcomePage() {
        return "welcome";
    }

    /**
     * Overrides the default Spring login page
     * @return
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }


    /**
     * Redirects user to login.html to display 'wrong user or password'
      */
    @GetMapping("/login-error")
    public String loginError(Model model) {
        model.addAttribute("loginError", true);
        return "login";
    }

    /**
     * Represents the landing page and renders the template based on recognised credentials
     * @param model
     * @return
     */
    @ClientRead
    @GetMapping("/authenticated")
    public String userLogin(Model model) {
        User user = userService.findByUsername(getUsername());
        model.addAttribute("userID", user.getId());
        model.addAttribute("user", getUsername());
        return "authenticated";
    }

    /**
     * Optional page in addition to the landing page
     * @param model
     * @return
     */
    @ClientRead
    @GetMapping("/userPage")
    public String userPage(Model model) {
        User user = userService.findByUsername(getUsername());
        model.addAttribute("userID", user.getId());
        model.addAttribute("user", getUsername());
        return "userPage";
    }

    /**
     * Lists all users on userPage
     * @param model
     * @return
     */
    @AdminRead
    @GetMapping("/listUsers")
    public String listUsers(Model model) {
        Set<User> userSet = new HashSet<>();
        //userSet is never null if user has one of the above roles
        userSet.addAll(userService.findAll());
        model.addAttribute("usersFound", userSet);
        User currentUser = userService.findByUsername(getUsername());
        model.addAttribute("userID", currentUser.getId());
        return "userPage";
    }

    /**
     * Frontend related user credentials page
     * @param model
     * @return
     */
    @AdminRead
    @GetMapping("/adminPage")
    public String frontendAdminPage(Model model) {
        Set<User> AdminUsers = userService.findAll().stream().filter(
                user -> user.getAdminUser() != null
        ).collect(Collectors.toSet());
        model.addAttribute("AdminUsersFound", AdminUsers);

        Set<User> ClientUsers = userService.findAll().stream().filter(
                user -> user.getClientUser() != null
        ).collect(Collectors.toSet());
        model.addAttribute("ClientUsersFound", ClientUsers);

        //current authenticated user details
        User user = userService.findByUsername(getUsername());
        model.addAttribute("userID", user.getId());
        model.addAttribute("user", getUsername());
        return "adminPage";
    }

    /**
     * This overrides the default GET logout page
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/logout")
    public String logoutPage(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/welcome";
    }

    /**
     * Resets the password to "username + 123"
     * @param userID
     * @param model
     * @return
     */
    @AdminUpdate
    @PostMapping("/resetPassword/{userID}")
    public String resetPassword(@PathVariable String userID, Model model) {
        if (userService.findById(Long.valueOf(userID)) != null) {
            User currentUser = userService.findById(Long.valueOf(userID));
            currentUser.setPassword(passwordEncoder.encode(currentUser.getUsername() + "123"));
            userService.save(currentUser);
            log.debug("Password was reset");
            model.addAttribute("user", getUsername());
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("confirmReset", "Password has been reset");
            if (currentUser.getAdminUser() != null){
                model.addAttribute("currentAdminUser", currentUser.getAdminUser());
                return "adminUpdate";
            } else {
                model.addAttribute("currentClientUser", currentUser.getClientUser());
                return "clientUpdate";
            }
        }
        log.debug("User with ID: " + userID + " not found");
        return "redirect:/adminPage";
    }

    @AdminUpdate
    @PutMapping("/changePassword/{userID}")
    public String updatePassword(@PathVariable String userID, @Valid @ModelAttribute("currentUser") User passwordChangeUser,
                                     BindingResult bindingResult) {
        if (userService.findById(Long.valueOf(userID)) != null) {
            if (passwordIsOK(bindingResult, true, passwordChangeUser.getPassword(), INVALID_PASSWORD)) {
                User saved = changeUserPassword(Long.valueOf(userID), passwordChangeUser);
                return "redirect:/" + userTypeUpdatePage(saved) + "/" + saved.getId();
            } else {
                User found = userService.findById(Long.valueOf(userID));
                return "redirect:/" + userTypeUpdatePage(found) + "/" + found.getId();
            }
        }
        log.debug("User with ID: " + userID + " not found");
        return "redirect:/adminPage";
    }

    @AdminDelete
    @DeleteMapping("/deleteUser/{userID}")
    public String deleteUser(@PathVariable String userID, Model model) {
        if (userService.findById(Long.valueOf(userID)) != null) {
            User currentUser = userService.findById(Long.valueOf(userID));
            if (Long.valueOf(userID).equals(userService.findByUsername(getUsername()).getId())) {
                log.debug("Cannot delete yourself");
                model.addAttribute("deniedDelete", "You are not permitted to delete your own account");
                model.addAttribute("returnURL", userTypeUpdatePage(currentUser) + "/" + userID);
                model.addAttribute("pageTitle", "previous page");
            } else {
                if (userTypeDelete(currentUser, userID)) {
                    model.addAttribute("confirmDelete", "User with username \"" + currentUser.getUsername()
                            + "\" successfully deleted");
                    model.addAttribute("returnURL", "adminPage");
                    model.addAttribute("pageTitle", "Admin page");
                } else {
                    model.addAttribute("deniedDelete", "User with username \"" + currentUser.getUsername()
                            + "\" was not deleted");
                    model.addAttribute("returnURL", "updateAdmin/" + currentUser.getId());
                    model.addAttribute("pageTitle", "previous page");
                }
            }
            return "confirmDelete";
        }
        log.debug("User with ID: " + userID + " not found");
        return "redirect:/adminPage";
    }

    // package-private helper methods called by related controller instances ---------------------------------------

    /**
     * inserts URL path related Strings dependent on the Usertype (AdminUser, or ClientUser)
     */
    @AdminRead
    String userTypeUpdatePage(User user) {
        if (user.getAdminUser() != null) {
            return "updateAdmin";
        } else
            return "updateClient";
    }

    /**
     * executes JPA User delete() dependent on the Usertype (AdminUser or ClientUser)
     */
    @AdminDelete
    boolean userTypeDelete(User user, String userID) {
        if (user.getAdminUser() != null) {
            deleteAdminUser(user.getId());
        } else
            deleteClientUser(user.getId());
        if (userService.findById(Long.valueOf(userID)) != null) {
            log.debug("User with ID: " + userID + " was not deleted");
            return false;
        } else
            return true;
    }

    @AdminRead
    String getUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    @AdminUpdate
    User changeUserPassword(Long userID, User userOnFile) {
        User found = userService.findById(userID);
        found.setPassword(passwordEncoder.encode(userOnFile.getPassword()));
        User saved = userService.save(found);
        log.debug("Password change for " + saved.getUsername() + " has been saved");
        return saved;
    }

    /**
     * Checks if a AdminUser/ClientUser name property is valid
     */
    @AdminCreate
    boolean newUserType_nameIsOK(BindingResult adminBindingResult, boolean checksOut, String adminUserName, String inputErrorMsg) {
        if (adminUserName == null || adminUserName.length() < 8) {
            log.debug(inputErrorMsg);
            adminBindingResult.getAllErrors().forEach(objectError -> log.debug(objectError.getDefaultMessage()));
            checksOut = false;
        }
        return checksOut;
    }

    /**
     * Checks if a User username property is valid
     */
    @AdminCreate
    boolean newUser_usernameIsOK(BindingResult userBindingResult, boolean checksOut, String username, String inputErrorMsg) {
        if (username == null || username.length() < 8) {
            //if the User username needs attention
            log.debug(inputErrorMsg);
            userBindingResult.getAllErrors().forEach(objectError -> log.debug(objectError.getDefaultMessage()));
            checksOut = false;
        }
        return checksOut;
    }

    @AdminUpdate
    boolean passwordIsOK(BindingResult userBindingResult, boolean checksOut, String password, String s) {
        if (password == null || password.length() < 8) {
            //if the password needs attention
            log.debug(s);
            userBindingResult.getAllErrors().forEach(objectError -> log.debug(objectError.getDefaultMessage()));
            checksOut = false;
        }
        return checksOut;
    }

    @AdminUpdate
    void syncAccountSettings(User currentUser, User userToBeUpdated) {
        userToBeUpdated.setAccountNonLocked(currentUser.isAccountNonLocked());
        userToBeUpdated.setAccountNonExpired(currentUser.isAccountNonExpired());
        userToBeUpdated.setCredentialsNonExpired(currentUser.isCredentialsNonExpired());
        userToBeUpdated.setEnabled(currentUser.isEnabled());
    }

    //assume here that all parameters are not null and not already on the DB
    @AdminUpdate
    void newAdminUser(AdminUser newAdminUser, User newUser) {
        Role adminRole = roleService.findByRoleName("ADMIN");
        AdminUser savedAdminUser = adminUserService.save(
                AdminUser.builder().adminUserName(newAdminUser.getAdminUserName()).build());
        User savedUser = userService.save(User.builder().adminUser(savedAdminUser)
                .username(newUser.getUsername()).password(passwordEncoder.encode(newUser.getPassword()))
                .role(adminRole).build());
        log.debug("New Admin name: " + savedUser.getAdminUser().getAdminUserName() + " with username" +
                savedUser.getUsername() + " and ID: " + savedUser.getId() + " added");
    }

    @AdminUpdate
    void newClientUser(ClientUser newClientUser, User newUser) {
        Role clientRole = roleService.findByRoleName("CLIENT");
        ClientUser savedClientUser = clientUserService.save(
                ClientUser.builder().clientUserName(newClientUser.getClientUserName()).build());
        User savedUser = userService.save(User.builder().clientUser(savedClientUser)
                .username(newUser.getUsername()).password(passwordEncoder.encode(newUser.getPassword()))
                .role(clientRole).build());
        log.debug("New Client name: " + savedUser.getClientUser().getClientUserName() + " with username" +
                savedUser.getUsername() + " and ID: " + savedUser.getId() + " added");
    }

    @AdminDelete
    void deleteAdminUser(Long userID) {
        //AdminUser to User is ManyToOne; do not delete AdminUser unless size == 0
        User toBeDeleted = userService.findById(userID);
        AdminUser adminUser = toBeDeleted.getAdminUser();

        //settle the mappings between User and AdminUser
        toBeDeleted.setAdminUser(null);
        adminUser.getUsers().removeIf(user -> user.getUsername().equals(toBeDeleted.getUsername()));
        adminUserService.save(adminUser);

        String adminUserName = adminUser.getAdminUserName();
        Long adminUserId = adminUser.getId();
        if (adminUser.getUsers().isEmpty()) {
            adminUserService.deleteById(adminUserId);
            log.debug("AdminUser, " + adminUserName + ", User set is now empty and has been deleted");
        } else {
            log.debug("AdminUser, " + adminUserName + ", has " + adminUser.getUsers().size() + " remaining Users associated");
        }

        userService.deleteById(userID);
    }

    @AdminDelete
    void deleteClientUser(Long userID) {
        // ClientUser to User is ManyToOne; do not delete ClientUser unless size == 0
        User toBeDeleted = userService.findById(userID);
        ClientUser clientUser = toBeDeleted.getClientUser();

        // settle the mappings between User and AdminUser
        toBeDeleted.setClientUser(null);
        clientUser.getUsers().removeIf(user -> user.getUsername().equals(toBeDeleted.getUsername()));
        clientUserService.save(clientUser);

        String clientUserName = clientUser.getClientUserName();
        Long clientUserId = clientUser.getId();
        if (clientUser.getUsers().isEmpty()) {
            clientUserService.deleteById(clientUserId);
            log.debug("ClientUser, " + clientUserName + ", User set is now empty and has been deleted");
        } else {
            log.debug("ClientUser, " + clientUserName + ", has " + clientUser.getUsers().size() + " remaining Users associated");
        }

        userService.deleteById(userID);
    }
}
