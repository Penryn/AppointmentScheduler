// 测试说明：验证用户服务的注册、查询、资料更新、密码修改和角色相关行为。
package com.example.slabiak.appointmentscheduler.service.user;

import com.example.slabiak.appointmentscheduler.dao.RoleRepository;
import com.example.slabiak.appointmentscheduler.dao.user.UserRepository;
import com.example.slabiak.appointmentscheduler.dao.user.customer.CorporateCustomerRepository;
import com.example.slabiak.appointmentscheduler.dao.user.customer.CustomerRepository;
import com.example.slabiak.appointmentscheduler.dao.user.customer.RetailCustomerRepository;
import com.example.slabiak.appointmentscheduler.dao.user.provider.ProviderRepository;
import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.Role;
import com.example.slabiak.appointmentscheduler.entity.user.User;
import com.example.slabiak.appointmentscheduler.entity.user.customer.CorporateCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.Customer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.RetailCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.model.ChangePasswordForm;
import com.example.slabiak.appointmentscheduler.model.UserForm;
import com.example.slabiak.appointmentscheduler.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CorporateCustomerRepository corporateCustomerRepository;

    @Mock
    private RetailCustomerRepository retailCustomerRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private int userId;
    private String password;
    private String passwordEncoded;
    private String userName;
    private String newPassword;
    private User user;
    private Optional<User> optionalUser;
    @BeforeEach
    public void initObjects() {
        userId = 1;
        passwordEncoded = "encodedpass";
        userName = "username";
        password = "password";
        newPassword = "newpassword";
        user = new User();
        user.setId(userId);
        user.setUserName(userName);
        optionalUser = Optional.of(user);
    }


    @Test
    public void shouldFindUserById() {
        when(userRepository.findById(userId)).thenReturn(optionalUser);
        // 检查点：验证该测试用例的预期结果。
        assertEquals(optionalUser.get(), userService.getUserById(userId));
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    public void shouldUpdateUserPassword() {
        doReturn(Optional.of(new User())).when(userRepository).findById(userId);
        ChangePasswordForm changePasswordForm = new ChangePasswordForm(userId);
        userService.updateUserPassword(changePasswordForm);
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        // 检查点：验证该测试用例的预期结果。
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(argumentCaptor.capture());
    }

    @Test
    public void shouldEncodeUserPasswordWhileUpdate() {
        User userToBeUpdated = new User();
        userToBeUpdated.setPassword(password);
        doReturn(Optional.of(userToBeUpdated)).when(userRepository).findById(userId);
        doReturn(passwordEncoded).when(passwordEncoder).encode(newPassword);
        ChangePasswordForm changePasswordForm = new ChangePasswordForm(userId);
        changePasswordForm.setCurrentPassword(password);
        changePasswordForm.setPassword(newPassword);

        userService.updateUserPassword(changePasswordForm);
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        // 检查点：验证该测试用例的预期结果。
        verify(passwordEncoder, times(1)).encode(newPassword);
        verify(userRepository, times(1)).save(argumentCaptor.capture());
        assertEquals(argumentCaptor.getValue().getPassword(), passwordEncoded);
    }

    @Test
    public void shouldFindUserBeUsername() {
        User user = new User();
        user.setId(userId);
        Optional<User> optionalUser = Optional.of(user);
        when(userRepository.findByUserName(userName)).thenReturn(optionalUser);
        // 检查点：验证该测试用例的预期结果。
        assertEquals(optionalUser.get(), userService.getUserByUsername(userName));
        verify(userRepository, times(1)).findByUserName(userName);
    }


    @Test
    public void shouldFindAllUsers() {
        List<User> users = new ArrayList<>();
        User user = new User();
        users.add(user);
        users.add(user);
        when(userRepository.findAll()).thenReturn(users);
        List<User> fetchedUsers = userService.getAllUsers();
        // 检查点：验证该测试用例的预期结果。
        assertEquals(fetchedUsers, users);
        assertEquals(fetchedUsers.size(), 2);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    public void shouldDeleteUserById() {
        userService.deleteUserById(userId);
        // 检查点：验证该测试用例的预期结果。
        verify(userRepository).deleteById(userId);
    }

    @Test
    public void shouldCheckWhetherUserExists() {
        when(userRepository.findByUserName(userName)).thenReturn(optionalUser, Optional.empty());

        Assertions.assertTrue(userService.userExists(userName));
        Assertions.assertFalse(userService.userExists(userName));

        // 检查点：验证该测试用例的预期结果。
        verify(userRepository, times(2)).findByUserName(userName);
    }

    @Test
    public void shouldThrowWhenUsersCannotBeFound() {
        when(userRepository.findById(10)).thenReturn(Optional.empty());
        when(customerRepository.findById(11)).thenReturn(Optional.empty());
        when(providerRepository.findById(12)).thenReturn(Optional.empty());
        when(retailCustomerRepository.findById(13)).thenReturn(Optional.empty());
        when(corporateCustomerRepository.findById(14)).thenReturn(Optional.empty());
        when(userRepository.findByUserName("missing")).thenReturn(Optional.empty());

        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.getUserById(10));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.getCustomerById(11));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.getProviderById(12));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.getRetailCustomerById(13));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.getCorporateCustomerById(14));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.getUserByUsername("missing"));
    }

    @Test
    public void shouldDelegateUserAndProviderListQueries() {
        PageRequest pageable = PageRequest.of(0, 10);
        Work work = new Work();
        Provider provider = new Provider();
        Customer customer = new Customer();
        RetailCustomer retailCustomer = new RetailCustomer();
        List<User> users = List.of(user);

        when(providerRepository.findAll()).thenReturn(List.of(provider));
        when(providerRepository.findListPage(pageable)).thenReturn(new PageImpl<>(List.of()));
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(customerRepository.findListPage(pageable)).thenReturn(new PageImpl<>(List.of()));
        when(retailCustomerRepository.findAll()).thenReturn(List.of(retailCustomer));
        when(userRepository.findByRoleName("ROLE_PROVIDER")).thenReturn(users);
        when(providerRepository.findAllWithRetailWorks()).thenReturn(List.of(provider));
        when(providerRepository.findAllWithCorporateWorks()).thenReturn(List.of(provider));
        when(providerRepository.findByWorks(work)).thenReturn(List.of(provider));

        // 检查点：验证该测试用例的预期结果。
        assertEquals(List.of(provider), userService.getAllProviders());
        assertEquals(0, userService.getProviderList(pageable).getTotalElements());
        assertEquals(List.of(customer), userService.getAllCustomers());
        assertEquals(0, userService.getCustomerList(pageable).getTotalElements());
        // 检查点：验证该测试用例的预期结果。
        assertEquals(List.of(retailCustomer), userService.getAllRetailCustomers());
        assertEquals(users, userService.getUsersByRoleName("ROLE_PROVIDER"));
        assertEquals(List.of(provider), userService.getProvidersWithRetailWorks());
        assertEquals(List.of(provider), userService.getProvidersWithCorporateWorks());
        // 检查点：验证该测试用例的预期结果。
        assertEquals(List.of(provider), userService.getProvidersByWork(work));
    }

    @Test
    public void shouldUpdateSpecificUserProfiles() {
        UserForm form = populatedUserForm();
        Provider provider = new Provider();
        RetailCustomer retailCustomer = new RetailCustomer();
        CorporateCustomer corporateCustomer = new CorporateCustomer();
        when(providerRepository.findById(form.getId())).thenReturn(Optional.of(provider));
        when(retailCustomerRepository.findById(form.getId())).thenReturn(Optional.of(retailCustomer));
        when(corporateCustomerRepository.findById(form.getId())).thenReturn(Optional.of(corporateCustomer));

        userService.updateProviderProfile(form);
        userService.updateRetailCustomerProfile(form);
        userService.updateCorporateCustomerProfile(form);

        // 检查点：验证该测试用例的预期结果。
        assertEquals(form.getFirstName(), provider.getFirstName());
        assertEquals(form.getEmail(), retailCustomer.getEmail());
        assertEquals(form.getCompanyName(), corporateCustomer.getCompanyName());
        verify(providerRepository).save(provider);
        // 检查点：验证该测试用例的预期结果。
        verify(retailCustomerRepository).save(retailCustomer);
        verify(corporateCustomerRepository).save(corporateCustomer);
    }

    @Test
    public void shouldSaveNewUsersWithEncodedPasswordsAndRoles() {
        UserForm form = populatedUserForm();
        Role retailRole = new Role("ROLE_CUSTOMER_RETAIL");
        Role customerRole = new Role("ROLE_CUSTOMER");
        Role corporateRole = new Role("ROLE_CUSTOMER_CORPORATE");
        Role providerRole = new Role("ROLE_PROVIDER");
        Work work = new Work();
        form.setWorks(List.of(work));
        when(passwordEncoder.encode(form.getPassword())).thenReturn(passwordEncoded);
        when(roleRepository.findByName("ROLE_CUSTOMER_RETAIL")).thenReturn(retailRole);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(customerRole);
        when(roleRepository.findByName("ROLE_CUSTOMER_CORPORATE")).thenReturn(corporateRole);
        when(roleRepository.findByName("ROLE_PROVIDER")).thenReturn(providerRole);

        userService.saveNewRetailCustomer(form);
        userService.saveNewCorporateCustomer(form);
        userService.saveNewProvider(form);

        ArgumentCaptor<RetailCustomer> retailCaptor = ArgumentCaptor.forClass(RetailCustomer.class);
        ArgumentCaptor<CorporateCustomer> corporateCaptor = ArgumentCaptor.forClass(CorporateCustomer.class);
        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        // 检查点：验证该测试用例的预期结果。
        verify(retailCustomerRepository).save(retailCaptor.capture());
        verify(corporateCustomerRepository).save(corporateCaptor.capture());
        verify(providerRepository).save(providerCaptor.capture());
        assertEquals(passwordEncoded, retailCaptor.getValue().getPassword());
        Assertions.assertTrue(retailCaptor.getValue().getRoles().containsAll(List.of(retailRole, customerRole)));
        // 检查点：验证该测试用例的预期结果。
        assertEquals("Acme", corporateCaptor.getValue().getCompanyName());
        Assertions.assertTrue(corporateCaptor.getValue().getRoles().containsAll(List.of(corporateRole, customerRole)));
        assertEquals(List.of(work), providerCaptor.getValue().getWorks());
        Assertions.assertTrue(providerCaptor.getValue().getRoles().contains(providerRole));
        Assertions.assertSame(providerCaptor.getValue(), providerCaptor.getValue().getWorkingPlan().getProvider());
    }

    @Test
    public void shouldReturnRoleCollectionsForEachUserType() {
        Role retailRole = new Role("ROLE_CUSTOMER_RETAIL");
        Role customerRole = new Role("ROLE_CUSTOMER");
        Role corporateRole = new Role("ROLE_CUSTOMER_CORPORATE");
        Role providerRole = new Role("ROLE_PROVIDER");
        when(roleRepository.findByName("ROLE_CUSTOMER_RETAIL")).thenReturn(retailRole);
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(customerRole);
        when(roleRepository.findByName("ROLE_CUSTOMER_CORPORATE")).thenReturn(corporateRole);
        when(roleRepository.findByName("ROLE_PROVIDER")).thenReturn(providerRole);

        Assertions.assertTrue(userService.getRolesForRetailCustomer().containsAll(List.of(retailRole, customerRole)));
        Assertions.assertTrue(userService.getRoleForCorporateCustomers().containsAll(List.of(corporateRole, customerRole)));
        Assertions.assertTrue(userService.getRolesForProvider().contains(providerRole));
    }

    private UserForm populatedUserForm() {
        UserForm form = new UserForm();
        form.setId(userId);
        form.setUserName(userName);
        form.setPassword(password);
        form.setFirstName("Ada");
        form.setLastName("Lovelace");
        form.setEmail("ada@example.com");
        form.setMobile("13800138000");
        form.setStreet("123 Test Street");
        form.setPostcode("100000");
        form.setCity("Beijing");
        form.setCompanyName("Acme");
        form.setVatNumber("123456789012345678");
        return form;
    }

}
