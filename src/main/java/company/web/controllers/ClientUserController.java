package company.web.controllers;

import company.exceptions.NotFoundException;
import company.model.security.ClientUser;
import company.model.security.User;
import company.services.securityServices.ClientUserService;
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
public class ClientUserController {

    private final UserService userService;
    private final ClientUserService clientUserService;

    private final UserController userController;

    private final String INVALID_USERNAME = "User's username length must be >= 8 characters";
    private final String INVALID_CLIENT_NAME = "ClientUser's name length must be >= 8 characters";

    //prevent the HTTP form POST from editing listed properties
    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @AdminRead
    @GetMapping("/createClient")
    public String getNewClient(Model model) {
        User user = User.builder().build();
        model.addAttribute("newUser", user);
        model.addAttribute("user", userController.getUsername());
        ClientUser clientUser = ClientUser.builder().build();
        model.addAttribute("newClient", clientUser);
        return "clientCreate";
    }

    //see postNewAdmin for comments
    @AdminCreate
    @PostMapping("/createClient")
    public String newClient(@Valid @ModelAttribute("newClient") ClientUser newClientUser,
                            BindingResult clientBindingResult, @Valid @ModelAttribute("newUser") User newUser,
                            BindingResult userBindingResult) {
        boolean checksOut = true;

        checksOut = userController.passwordIsOK(userBindingResult, checksOut, newUser.getPassword(), UserController.INVALID_PASSWORD);
        checksOut = userController.newUser_usernameIsOK(userBindingResult, checksOut, newUser.getUsername(), INVALID_USERNAME);
        checksOut = userController.newUserType_nameIsOK(clientBindingResult, checksOut, newClientUser.getClientUserName(),
                INVALID_CLIENT_NAME);

        if (!checksOut) {
            return "clientCreate";
        }

        User userFound;
        ClientUser clientUserFound;
        if (userService.findByUsername(newUser.getUsername()) != null) {
            userFound = userService.findByUsername(newUser.getUsername());
            if (clientUserService.findByClientUserName(newClientUser.getClientUserName()) != null) {
                clientUserFound = clientUserService.findByClientUserName(newClientUser.getClientUserName());
                if (clientUserFound.getUsers().stream().anyMatch(user ->
                        user.getUsername().equals(userFound.getUsername()))) {
                    log.debug("ClientUser is already registered with the given User");
                    clientBindingResult.rejectValue("clientUserName", "exists",
                            "ClientUser provided is already registered with given User. Please change the ClientUser name.");
                    return "clientCreate";
                }
            }
        }

        userController.newClientUser(newClientUser, newUser);
        return "redirect:/adminPage";
    }

    @AdminUpdate
    @GetMapping("/updateClient/{clientUserID}")
    public String getUpdateClient(Model model, @PathVariable String clientUserID) {
        if (userService.findById(Long.valueOf(clientUserID)) == null) {
            log.debug("User with ID " + clientUserID + " not found");
            throw new NotFoundException();
        }
        User user = userService.findById(Long.valueOf(clientUserID));
        if (user.getClientUser() == null) {
            log.debug("No clientUser associated with given user");
            return "redirect:/adminPage";
        } else {
            ClientUser clientUser = user.getClientUser();
            model.addAttribute("user", userController.getUsername());
            model.addAttribute("currentUser", user);
            model.addAttribute("currentClientUser", clientUser);
            return "clientUpdate";
        }
    }

    @AdminUpdate
    @PutMapping("/updateClient/{clientUserID}")
    public String updateClient(@PathVariable String clientUserID,
                               @Valid @ModelAttribute("currentUser") User currentUser, BindingResult userBindingResult,
                               @Valid @ModelAttribute("currentClientUser") ClientUser currentClientUser,
                               BindingResult clientBindingResult, Model model) {
        if (userService.findById(Long.valueOf(clientUserID)) == null) {
            throw new NotFoundException("User with given ID not found. No updates committed.");
        }

        User userToBeUpdated = userService.findById(Long.valueOf(clientUserID));
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

        //either the adminUser name field is empty or is already on file
        if (clientBindingResult.hasErrors()) {
            model.addAttribute("clientUserNameError", INVALID_CLIENT_NAME);
            allGood = false;
        } else if (clientUserService.findByClientUserName(currentClientUser.getClientUserName()) == null
                || userToBeUpdated.getClientUser().getClientUserName().equals(currentClientUser.getClientUserName())) {
            userToBeUpdated.getClientUser().setClientUserName(currentClientUser.getClientUserName());
        } else {
            model.addAttribute("clientUserExists", "ClientUser with given name already exists");
            allGood = false;
        }

        //models needed to set ID of POST path
        if (!allGood) {
            userToBeUpdated.setUsername(currentUser.getUsername());
            userToBeUpdated.getClientUser().setClientUserName(currentClientUser.getClientUserName());
            model.addAttribute("user", userController.getUsername());
            model.addAttribute("currentUser", userToBeUpdated);
            model.addAttribute("currentClientUser", userToBeUpdated.getClientUser());
            return "clientUpdate";
        }

        userController.syncAccountSettings(currentUser, userToBeUpdated);

        //save changes
        User saved = userService.save(userToBeUpdated);
        log.debug("Username: " + saved.getUsername() + ", clientUser name: " + saved.getClientUser().getClientUserName() +
                " saved");
        model.addAttribute("ClientUserSaved", "Updates applied successfully");
        model.addAttribute("currentUser", saved);
        model.addAttribute("currentClientUser", saved.getClientUser());
        return "clientUpdate";
    }
}
