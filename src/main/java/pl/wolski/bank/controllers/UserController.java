package pl.wolski.bank.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import pl.wolski.bank.controllers.commands.UserFilter;
import pl.wolski.bank.models.*;
import pl.wolski.bank.repositories.RoleRepository;
import pl.wolski.bank.services.BankAccountService;
import pl.wolski.bank.services.NotificationService;
import pl.wolski.bank.services.TransactionService;
import pl.wolski.bank.services.UserService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

@Log4j2
@Controller
@SessionAttributes(names = {"user", "userAccount"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping(path = "/index")
    //@RequestMapping(path = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    public String home(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (!(auth instanceof AnonymousAuthenticationToken)) {
            /* The user is logged in :slight_smile: */
            User user = userService.findByUsername(((UserDetails)principal).getUsername());

            List<Role> userRole = (userService.findRoleByUser(user));

            boolean isAdmin = false;
            boolean isEmplo = false;

            for (Role role : userRole) {
                if (role.getType() == Role.Types.ROLE_ADMIN){
                    isAdmin = true;
                }
                else if (role.getType() == Role.Types.ROLE_EMPLOYEE) {
                    isEmplo = true;
                }
            }

            if (isAdmin) {
                return "redirect:/creditApplicationsList";
            } else if (isEmplo) {
                return "redirect:/users";
            } else {
                List<Transaction> transactions = transactionService.findUserTop5Transactions(
                        bankAccountService.getUserAccount(user).getBankAccountNumber(),
                        bankAccountService.getUserAccount(user).getBankAccountNumber());
                model.addAttribute("transactions", transactions);
                model.addAttribute("userAccount", bankAccountService.getUserAccount(user));

                return "index";
            }
        }
        return "loginForm";
    }

    @ModelAttribute("searchCommand")
    public UserFilter getSimpleSearch(){
        log.info("Załadowano searchCommand");
        UserFilter userFilter = new UserFilter();
        userFilter.setPhrase("");
        userFilter.setPersonalIdentificationNumber("");
        return userFilter;
    }

    @Secured({"ROLE_ADMIN", "ROLE_EMPLOYEE"})
    @GetMapping(path = "/user")
    //@RequestMapping(path = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    public String user(Model model,
                       Long id) {
        User user = userService.findById(id);

        model.addAttribute("user", user);

        List<Transaction> transactions = transactionService.findUserTop5Transactions(
                bankAccountService.getUserAccount(userService.findByUsername(user.getUsername())).getBankAccountNumber(),
                bankAccountService.getUserAccount(userService.findByUsername(user.getUsername())).getBankAccountNumber());
        model.addAttribute("transactions", transactions);
        model.addAttribute("userAccounts", bankAccountService.findUserAccounts(user));


        return "user";
    }

    @GetMapping(value="/users", params = {"all"})
    public String resetUserList(@ModelAttribute("searchCommand") UserFilter search){
        search.clear();
        return "redirect:users";
    }


    @Secured("ROLE_EMPLOYEE")
    @RequestMapping(value="/users", method = {RequestMethod.GET, RequestMethod.POST})
    public String showUserList(Model model, @PageableDefault(value = 10) Pageable pageable, @Valid @ModelAttribute("searchCommand") UserFilter search){
        String role = (roleRepository.findRoleByType(Role.Types.ROLE_USER)).getType().name();
        String pageableString = pageable.toString();
        log.info("Pageable: " + pageableString);
        model.addAttribute("userListPage", userService.getAllUsersByTypeAndPhrase(search, pageable, role));

        return "users";
    }

    /*

    @Secured("ROLE_EMPLOYEE")
    @PostMapping(value="/users")
    public String showSearchUserList(Model model, Pageable pageable, @Valid @ModelAttribute("searchCommand") UserFilter search){
        String role = (roleRepository.findRoleByType(Role.Types.ROLE_USER)).getType().name();
        model.addAttribute("userListPage", userService.getAllUsersByTypeAndPhrase(search, pageable, role));

        return "users";
    }

     */

    @ModelAttribute("notificationCounter")
    public int notificationCounter(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        User user = userService.findByUsername(((UserDetails)principal).getUsername());
        List<Notification> notificationList = notificationService.findByUserAndWasRead(user, false);
        log.info("Ładowanie listy " + notificationList.size() + " kont bankowych ");
        return notificationList.size();
    }

    /*
    @ModelAttribute("transactions")
    public List<Transaction> loadTransactions(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();

        log.info("Ładowanie listy " + transactions.size() + " transakcji ");
        return transactions;
    }
    */

    @InitBinder
    public void initBinder(WebDataBinder binder) {//Rejestrujemy edytory właściwości
        DecimalFormat numberFormat = new DecimalFormat("#0.00");
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setGroupingUsed(false);
        CustomNumberEditor priceEditor = new CustomNumberEditor(Float.class, numberFormat, true);
    }

}
