package com.example.slabiak.appointmentscheduler.model;

import com.example.slabiak.appointmentscheduler.entity.Work;
import com.example.slabiak.appointmentscheduler.entity.user.User;
import com.example.slabiak.appointmentscheduler.entity.user.customer.CorporateCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.customer.RetailCustomer;
import com.example.slabiak.appointmentscheduler.entity.user.provider.Provider;
import com.example.slabiak.appointmentscheduler.validation.FieldsMatches;
import com.example.slabiak.appointmentscheduler.validation.UniqueUsername;
import com.example.slabiak.appointmentscheduler.validation.groups.*;

import jakarta.validation.constraints.*;
import java.util.List;

@FieldsMatches(field = "password", matchingField = "matchingPassword", groups = {CreateUser.class})
public class UserForm {

    @NotNull(groups = {UpdateUser.class})
    @Min(value = 1, groups = {UpdateUser.class})
    private int id;

    @UniqueUsername(groups = {CreateUser.class})
    @Size(min = 5, max = 15, groups = {CreateUser.class}, message = "用户名长度应为 5-15 个字符")
    @NotBlank(groups = {CreateUser.class}, message = "用户名不能为空")
    private String userName;

    @Size(min = 5, max = 15, groups = {CreateUser.class}, message = "密码长度应为 5-15 个字符")
    @NotBlank(groups = {CreateUser.class}, message = "密码不能为空")
    private String password;

    @Size(min = 5, max = 15, groups = {CreateUser.class}, message = "密码长度应为 5-15 个字符")
    @NotBlank(groups = {CreateUser.class}, message = "确认密码不能为空")
    private String matchingPassword;

    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "名不能为空")
    private String firstName;

    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "姓不能为空")
    private String lastName;

    @Email(groups = {CreateUser.class, UpdateUser.class}, message = "邮箱格式不正确")
    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "邮箱不能为空")
    private String email;

    @Pattern(groups = {CreateUser.class, UpdateUser.class}, regexp = "1[3-9][0-9]{9}", message = "请输入有效的中国大陆手机号")
    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "手机号不能为空")
    private String mobile;

    @Size(groups = {CreateUser.class, UpdateUser.class}, min = 5, max = 30, message = "街道地址长度应为 5-30 个字符")
    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "街道地址不能为空")
    private String street;

    @Pattern(groups = {CreateUser.class, UpdateUser.class}, regexp = "[0-9]{6}", message = "请输入 6 位中国邮政编码")
    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "邮编不能为空")
    private String postcode;

    @NotBlank(groups = {CreateUser.class, UpdateUser.class}, message = "城市不能为空")
    private String city;

    /*
     * CorporateCustomer only:
     * */
    @NotBlank(groups = {CreateCorporateCustomer.class, UpdateCorporateCustomer.class}, message = "公司名称不能为空")
    private String companyName;

    @Pattern(groups = {CreateCorporateCustomer.class, UpdateCorporateCustomer.class}, regexp = "[0-9A-Z]{18}", message = "请输入 18 位统一社会信用代码")
    @NotBlank(groups = {CreateCorporateCustomer.class, UpdateCorporateCustomer.class}, message = "统一社会信用代码不能为空")
    private String vatNumber;

    /*
     * Provider only:
     * */
    @NotNull(groups = {CreateProvider.class, UpdateProvider.class})
    private List<Work> works;


    public UserForm() {
    }

    public UserForm(User user) {
        this.setId(user.getId());
        this.setUserName(user.getUserName());
        this.setFirstName(user.getFirstName());
        this.setLastName(user.getLastName());
        this.setEmail(user.getEmail());
        this.setCity(user.getCity());
        this.setStreet(user.getStreet());
        this.setPostcode(user.getPostcode());
        this.setMobile(user.getMobile());
    }

    public UserForm(Provider provider) {
        this((User) provider);
        this.setWorks(provider.getWorks());
    }

    public UserForm(RetailCustomer retailCustomer) {
        this((User) retailCustomer);
    }

    public UserForm(CorporateCustomer corporateCustomer) {
        this((User) corporateCustomer);
        this.setCompanyName(corporateCustomer.getCompanyName());
        this.setVatNumber(corporateCustomer.getVatNumber());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMatchingPassword() {
        return matchingPassword;
    }

    public void setMatchingPassword(String matchingPassword) {
        this.matchingPassword = matchingPassword;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<Work> getWorks() {
        return works;
    }

    public void setWorks(List<Work> works) {
        this.works = works;
    }

}
