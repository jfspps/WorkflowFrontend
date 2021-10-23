package company.web.controllers;

import company.exceptions.NotFoundException;
import company.model.security.AdminUser;
import company.model.security.User;
import company.services.securityServices.AdminUserService;
import company.services.securityServices.UserService;
import company.web.permissionAnnot.AdminCreate;
import company.web.permissionAnnot.AdminRead;
import company.web.permissionAnnot.AdminUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RequiredArgsConstructor
@Controller
public class AdminUserController {

    private final UserService userService;
    private final AdminUserService adminUserService;

    private final String INVALID_USERNAME = "User's username length must be >= 8 characters";
    private final String INVALID_ADMIN_NAME = "AdminUser's name length must be >= 8 characters";

    private final UserController userController;

    // prevent the HTTP form POST from editing listed properties
    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @AdminRead
    @GetMapping("/createAdmin")
    public String getNewAdmin(Model model) {
        User user = User.builder().build();
        model.addAttribute("newUser", user);
        model.addAttribute("user", userController.getUsername());
        AdminUser adminUser = AdminUser.builder().build();
        model.addAttribute("newAdmin", adminUser);
        return "adminCreate";
    }

    @AdminCreate
    @PostMapping("/createAdmin")
    public String newAdmin(@Valid @ModelAttribute("newAdmin") AdminUser newAdminUser,
                           BindingResult adminBindingResult, @Valid @ModelAttribute("newUser") User newUser,
                           BindingResult userBindingResult) {
        boolean checksOut = true;

        checksOut = userController.passwordIsOK(userBindingResult, checksOut, newUser.getPassword(), UserController.INVALID_PASSWORD);
        checksOut = userController.newUser_usernameIsOK(userBindingResult, checksOut, newUser.getUsername(), INVALID_USERNAME);
        checksOut = userController.newUserType_nameIsOK(adminBindingResult, checksOut, newAdminUser.getAdminUserName(), INVALID_ADMIN_NAME);

        if (!checksOut) {
            return "adminCreate";
        }

        // At present, this project saves new AdminUser and User concurrently (treated as one entity) since we require a User password.
        // Different AdminUsers associated with the same User would require more functionality not offered here.
        // We proceed here assuming that different AdminUsers can be associated with the same User.

        // New Users, with given Roles, are instantiated before AdminUsers. One AdminUser is associated with many Users.
        // All User usernames and hence Users are unique. Check that the new AdminUser is not registering with a User it is already
        // associated with
        User userFound;
        AdminUser adminUserFound;
        if (userService.findByUsername(newUser.getUsername()) != null) {
            // User is already on file
            userFound = userService.findByUsername(newUser.getUsername());
            if (adminUserService.findByAdminUserName(newAdminUser.getAdminUserName()) != null) {
                // AdminUser is also on file
                adminUserFound = adminUserService.findByAdminUserName(newAdminUser.getAdminUserName());
                if (adminUserFound.getUsers().stream().anyMatch(user ->
                        user.getUsername().equals(userFound.getUsername()))) {
                    // already registered/associated (only option is to change the AdminUserName
                    log.debug("AdminUser is already registered with the given User");
                    adminBindingResult.rejectValue("adminUserName", "exists",
                            "AdminUser provided is already registered with given User. Please change the AdminUser name.");
                    return "adminCreate";
                }
                // not currently registered with given User (can save current form data)
            }
            // AdminUser not found (can save current form data)
        }
        // User not found (can save current form data)

        //all checks complete
        userController.newAdminUser(newAdminUser, newUser);
        return "redirect:/adminPage";
    }

    @AdminUpdate
    @GetMapping("/updateAdmin/{adminUserID}")
    public String getUpdateAdmin(Model model, @PathVariable String adminUserID) {
        if (userService.findById(Long.valueOf(adminUserID)) == null) {
            log.debug("User with ID " + adminUserID + " not found");
            throw new NotFoundException();
        }
        User user = userService.findById(Long.valueOf(adminUserID));
        // guard against wrong adminUser by user ID
        if (user.getAdminUser() == null) {
            log.debug("No adminUser associated with given user");
            return "redirect:/adminPage";
        } else {
            AdminUser adminUser = user.getAdminUser();
            model.addAttribute("user", userController.getUsername());
            model.addAttribute("currentUser", user);
            model.addAttribute("currentAdminUser", adminUser);
            return "adminUpdate";
        }
    }

    @AdminUpdate
    @PutMapping("/updateAdmin/{adminUserID}")
    public String updateAdminWithID(@PathVariable String adminUserID,
                                    @Valid @ModelAttribute("currentUser") User currentUser, BindingResult userBindingResult,
                                    @Valid @ModelAttribute("currentAdminUser") AdminUser currentAdminUser,
                                    BindingResult adminBindingResult, Model model) {
        if (userService.findById(Long.valueOf(adminUserID)) == null) {
            throw new NotFoundException("User with given ID not found. No updates committed.");
        }

        User userToBeUpdated = userService.findById(Long.valueOf(adminUserID));
        // either the username is empty or is already on file
        boolean allGood = true;
        if (userBindingResult.hasErrors()) {
            model.addAttribute("usernameError", INVALID_USERNAME);
            allGood = false;
        } else if (userService.findByUsername(currentUser.getUsername()) == null
                || userToBeUpdated.getUsername().equals(currentUser.getUsername())) {
            userToBeUpdated.setUsername(currentUser.getUsername());
        } else {
            model.addAttribute("usernameExists", "Username already taken");
            allGood = false;
        }

        // either the adminUser name field is empty or is already on file
        if (adminBindingResult.hasErrors()) {
            model.addAttribute("adminUserNameError", INVALID_ADMIN_NAME);
            allGood = false;
        } else if (adminUserService.findByAdminUserName(currentAdminUser.getAdminUserName()) == null
                || userToBeUpdated.getAdminUser().getAdminUserName().equals(currentAdminUser.getAdminUserName())) {
            userToBeUpdated.getAdminUser().setAdminUserName(currentAdminUser.getAdminUserName());
        } else {
            model.addAttribute("adminUserExists", "AdminUser with given name already exists");
            allGood = false;
        }

        // models needed to set ID of POST path
        if (!allGood) {
            userToBeUpdated.setUsername(currentUser.getUsername());
            userToBeUpdated.getAdminUser().setAdminUserName(currentAdminUser.getAdminUserName());
            model.addAttribute("user", userController.getUsername());
            model.addAttribute("currentUser", userToBeUpdated);
            model.addAttribute("currentAdminUser", userToBeUpdated.getAdminUser());
            return "adminUpdate";
        }

        // sync. account related settings
        userController.syncAccountSettings(currentUser, userToBeUpdated);

        // save changes
        User saved = userService.save(userToBeUpdated);
        log.debug("Username: " + saved.getUsername() + ", adminUser name: " + saved.getAdminUser().getAdminUserName() +
                " saved");
        model.addAttribute("AdminUserSaved", "Updates applied successfully");
        model.addAttribute("currentUser", saved);
        model.addAttribute("currentAdminUser", saved.getAdminUser());
        return "adminUpdate";
    }
}
