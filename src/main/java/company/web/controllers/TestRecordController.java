package company.web.controllers;

import company.exceptions.NotFoundException;
import company.model.TestRecord;
import company.model.security.User;
import company.services.TestRecordService;
import company.services.securityServices.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Set;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TestRecordController {

    private final TestRecordService testRecordService;
    private final UserService userService;

    //prevent the HTTP form POST from editing listed properties
    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields("id");
    }

    @ModelAttribute("testRecordSet")
    public Set<TestRecord> populateTestRecords() {
        return testRecordService.findAll();
    }

    //pass the authenticated user from the context with @AuthenticationPrincipal
    @GetMapping("/testRecord")
    public String getCRUDpage(@AuthenticationPrincipal User user, Model model) {
        // the authenticated user is injected into the current session. If the authenticated user belongs to a
        // admin User (user.getAdminUser) then all records are presented
        model.addAttribute("testRecords", testRecordService.findAll());
        model.addAttribute("userID", user.getId());
        return "testRecord";
    }

    @GetMapping("/createTestRecord")
    public String getNewTestRecord(Model model) {
        model.addAttribute("newTestRecord", new TestRecord());
        model.addAttribute("clientUser", new User());
        return "testRecordCreate";
    }

    @PostMapping("/createTestRecord")
    public String newTestRecord(@Valid @ModelAttribute("newTestRecord") TestRecord testRecord, BindingResult TRbindingResult,
                                       @Valid @ModelAttribute("clientUser") User clientUser, BindingResult GbindingResult) {
        if (!TestRecord_ClientNameFields_AreEmpty(testRecord, clientUser)) {
            if (userService.findByUsername(clientUser.getUsername()) != null)  {
                // check if the testRecord, by the submitted form, already exists
                if (testRecordService.findByRecordName(testRecord.getRecordName()) != null) {
                    // another testRecord with the same record name found, see if the current client is its 'owner'
                    TestRecord TRfound = testRecordService.findByRecordName(testRecord.getRecordName());
                    User Gfound = userService.findByUsername(clientUser.getUsername());
                    if (!testRecordBelongsToClient(TRfound, Gfound)) {
                        saveTestRecordWithClient(testRecord, clientUser);
                    } else {
                        log.debug("TestRecord is already associated with the provided client details. No changes made.");
                        TRbindingResult.rejectValue("recordName", "exists", "Supplied testRecord already exists");
                        return "testRecordCreate";
                    }
                } else {
                    log.debug("TestRecord not found, saving new testRecord");
                    saveTestRecordWithClient(testRecord, clientUser);
                }
            } else {
                log.debug("Client with given (User) username not found");
                GbindingResult.rejectValue("username", "notFound", "Client username not found");
                return "testRecordCreate";
            }
        } else {
            log.debug("Both username and record name fields are empty");
            printTestRecordCreateErrors(TRbindingResult, GbindingResult);
            return "testRecordCreate";
        }
        return "redirect:/testRecord";
    }

    @GetMapping("/testRecord/{id}")
    public String getTestRecordById(@PathVariable String id, Model model) {
        if (testRecordService.findById(Long.valueOf(id)) == null) {
            log.debug("TestRecord with ID: " + id + " not found");
            throw new NotFoundException("TestRecord with ID: " + id + " not found");
        }
        TestRecord found = testRecordService.findById(Long.valueOf(id));
        User client = found.getUser();
        model.addAttribute("client", client);
        model.addAttribute("testRecord", found);
        return "testRecordUpdate";
    }

    @PutMapping("/{clientId}/updateTestRecord/{testRecordID}")
    public String updateTestRecord(@Valid @ModelAttribute("testRecord") TestRecord testRecord, BindingResult TRbindingResult,
                                       @PathVariable String testRecordID, @PathVariable String clientId, Model model) {
        if (testRecord.getRecordName().isEmpty()) {
            log.debug("Record name entry is empty");
            TestRecord found = testRecordService.findById(Long.valueOf(testRecordID));

            model.addAttribute("error", "Record name required");
            model.addAttribute("client", found.getUser());
            model.addAttribute("testRecord", found);
            return "testRecordUpdate";
        }
        if (userService.findById(Long.valueOf(clientId)) == null) {
            log.debug("Valid User (client) ID are required");
            return "redirect:/testRecord";
        } else {
            log.debug("Client ID: " + clientId + ", testRecord string: " + testRecord.getRecordName() + " submitted");
            // check whether the new testRecord as per the form already exists
            if (testRecordService.findByRecordName(testRecord.getRecordName()) != null) {
                // check whether the testRecord as per the form is already assigned to the current client
                TestRecord testRecordByForm = testRecordService.findByRecordName(testRecord.getRecordName());
                User currentClient = userService.findById(Long.valueOf(clientId));

                if (!testRecordBelongsToClient(testRecordByForm, currentClient)) {
                    updateTestRecord_recordName(testRecord, testRecordID);
                } else {
                    log.debug("The current client is already assigned a testRecord with the given values. " +
                            "No changes made");
                    TRbindingResult.rejectValue("recordName", "exists", "Supplied testRecord already exists");
                    model.addAttribute("testRecord", testRecordByForm);
                    model.addAttribute("client", currentClient);
                    model.addAttribute("error", "The testRecord supplied is already associated with client, "
                            + currentClient.getUsername());
                    return "testRecordUpdate";
                }
            } else {
                //testRecord doesn't exist, so save to Client's account
                updateTestRecord_recordName(testRecord, testRecordID);
            }
        }
        return "redirect:/testRecord";
    }

    @DeleteMapping("/deleteTestRecord/{testRecordID}")
    public String deleteTestRecord(@PathVariable String testRecordID) {
        if (testRecordService.findById(Long.valueOf(testRecordID)) == null) {
            log.debug("No record on file with id: " + testRecordID + ", nothing deleted");
            return "redirect:/testRecord";
        } else {
            User associatedUser = testRecordService.findById(Long.valueOf(testRecordID)).getUser();
            testRecordService.deleteTestRecordAndUpdateUser(Long.valueOf(testRecordID), associatedUser);
        }
        return "redirect:/testRecord";
    }

    // 'ancillary' methods

    private void printTestRecordCreateErrors(BindingResult TRbindingResult, BindingResult GbindingResult) {
        TRbindingResult.getAllErrors().forEach(objectError -> {
            log.debug("testRecord: " + objectError.toString());  //use to build custom messages
        });
        GbindingResult.getAllErrors().forEach(objectError -> {
            log.debug("ClientUser: " + objectError.toString());
        });
        log.debug("TestRecord not saved to DB");
    }

    private void printTestRecordCreateErrors(BindingResult TRbindingResult) {
        TRbindingResult.getAllErrors().forEach(objectError -> {
            log.debug("testRecord: " + objectError.toString());  //use to build custom messages
        });
        log.debug("TestRecord not saved to DB");
    }

    private void saveTestRecordWithClient(TestRecord testRecord, User clientUser) {
        TestRecord saved = testRecordService.createTestRecord(testRecord.getRecordName(), clientUser.getUsername());
        log.debug("Received clientUser with id: " + saved.getUser().getId()
                + " and username: " + saved.getUser().getUsername());
    }

    private boolean testRecordBelongsToClient(TestRecord TRfound, User gfound) {
            return TRfound.getUser().getId().equals(gfound.getId());
    }

    private boolean TestRecord_ClientNameFields_AreEmpty(TestRecord testRecord, User clientUser) {
        return testRecord.getRecordName().isEmpty() || clientUser.getUsername().isEmpty();
    }

    private void updateTestRecord_recordName(TestRecord testRecord, String testRecordID) {
        TestRecord testRecordOnFile = testRecordService.findById(Long.valueOf(testRecordID));
        testRecordService.updateTestRecord(Long.valueOf(testRecordID),
                testRecordOnFile.getUser().getId(), testRecord.getRecordName());
    }
}
