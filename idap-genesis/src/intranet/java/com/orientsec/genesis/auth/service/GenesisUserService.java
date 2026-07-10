package com.orientsec.genesis.auth.service;

import com.orientsec.genesis.auth.common.model.Menu;
import com.orientsec.genesis.auth.common.model.Role;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.ldap.service.common.model.Department;
import com.orientsec.genesis.ldap.service.common.model.Employee;
import java.util.List;

/** Project-facing Genesis user and organization service contract. */
public interface GenesisUserService {

    User getProjectUserByName(String userId);

    String getUserCNName(String userId);

    User getCurrentUser();

    Employee getCurrentEmployee();

    List<Menu> getUserMenus();

    void logout();

    List<Employee> getUser(String name);

    List<Department> getDepartment(int type);

    Employee getEmployeeByName(String userName);

    List<Role> getUserRoles(Long userId);

    String getProjectKey();
}
