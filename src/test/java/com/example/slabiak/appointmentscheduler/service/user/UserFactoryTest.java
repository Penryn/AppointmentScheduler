// 测试工具说明：提供用户相关测试所需的样本表单数据。
package com.example.slabiak.appointmentscheduler.service.user;

import com.example.slabiak.appointmentscheduler.model.UserForm;

public class UserFactoryTest {

    public static UserForm prepareSampleUserForm() {
        UserForm userForm = new UserForm();
        userForm.setCity("Wroclaw");
        userForm.setEmail("example@example.com");
        userForm.setFirstName("Tomasz");
        userForm.setLastName("Nowak");
        userForm.setMobile("123456");
        userForm.setPostcode("12-234");
        userForm.setStreet("Street");
        userForm.setUserName("sampleUser");
        userForm.setPassword("password");
        return userForm;
    }
}
