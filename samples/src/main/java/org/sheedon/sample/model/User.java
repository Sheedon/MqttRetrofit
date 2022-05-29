package org.sheedon.sample.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/25 9:53
 */
public class User {
    private String loginName;
    private String password;

    private Test test;

    private List<Test> tests;

    public String getLoginName() {
        return loginName;
    }

    public String getPassword() {
        return password;
    }

    public Test getTest() {
        return test;
    }

    public List<Test> getTests() {
        return tests;
    }

    public static User build(String loginName, String password) {
        User user = new User();

        user.loginName = loginName;
        user.password = password;
        user.test = Test.build();

        user.tests = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            user.tests.add(Test.build());
        }
        return user;
    }

    @Override
    public String toString() {
        return "User{" +
                "loginName='" + loginName + '\'' +
                ", password='" + password + '\'' +
                ", test=" + test +
                ", tests=" + tests +
                '}';
    }

    public static class Test {
        private String test;

        public static Test build() {
            Test test = new Test();

            test.test = "hahah";
            return test;
        }
    }
}
