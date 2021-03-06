package org.nix.web.controller.process;

import org.apache.log4j.Logger;
import org.hibernate.PropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.nix.annotation.AuthPassport;
import org.nix.annotation.ValidatePermission;
import org.nix.dao.service.ResourcesService;
import org.nix.dao.service.RoleService;
import org.nix.dao.service.UserService;
import org.nix.domain.entity.Resources;
import org.nix.domain.entity.Role;
import org.nix.domain.entity.User;
import org.nix.domain.entity.dto.ResultDto;
import org.nix.domain.entity.dto.overtime.PresonalOvertimeInformationDTO;
import org.nix.domain.entity.dto.user.UserDetailDTO;
import org.nix.domain.entity.dto.user.UserInformationDTO;
import org.nix.domain.entity.dto.user.UserListDTO;
import org.nix.domain.entity.entitybuild.ResourcesBuild;
import org.nix.domain.entity.entitybuild.UserBuild;
import org.nix.exception.AccountNumberException;
import org.nix.exception.AuthorizationException;
import org.nix.exception.IdentityOverdueException;
import org.nix.exception.SelectException;
import org.nix.utils.SessionKey;
import org.nix.utils.SystemUtil;
import org.nix.web.controller.utils.ResultMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Create by zhangpe0312@qq.com on 2018/3/10.
 * <p>
 * 用户接口
 */
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserInformationDTO userInformation;

    @Autowired
    private PresonalOvertimeInformationDTO presonalOvertimeInformation;

    @Autowired
    private RoleService roleService;

    @Autowired
    private UserListDTO userListDTO;

    @Autowired
    private UserDetailDTO userDetailDTO;

    @Autowired
    private ResourcesService resourcesService;


    //日志记录
    private static Logger logger = Logger.getLogger(UserController.class);

    /**
     * 登陆接口
     * <p>
     * 通用接口
     *
     * @param userName 用户警号
     * @param password 用户密码
     * @param session  用户进程
     * @return 查询结果
     * @throws AccountNumberException 用户账号密码错误异常
     * @throws NullPointerException   空指针异常
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public Map<String, Object> login(@RequestParam("userName") String userName,
                                     @RequestParam("password") String password,
                                     HttpSession session) throws AccountNumberException, NullPointerException {

        User user = userService.login(userName, password);
        session.setAttribute(SessionKey.USER, user);
        String roleName = user.getRole().getName();
        logger.info(user.getName() + "登陆成功 角色为" + roleName);

        return new ResultMap()
                .resultSuccess()
                .appendParameter(ResultMap.ROLE_CLASS, roleName.equals("普通用户") ? 0 : 1)
                .send();
    }

    /**
     * 用户注册接口
     * <p>
     * 通用接口
     *
     * @param serialNumber 警号
     * @param password     用户密码
     * @return 返回结果
     * @throws NullPointerException         空指针异常
     * @throws PropertyValueException       数据字段为空
     * @throws ConstraintViolationException 数据插入违反唯一约束
     */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public Map<String, Object> register(@RequestParam("serialNumber") String serialNumber,
                                        @RequestParam("password") String password,
                                        @RequestParam("userName") String userName)
            throws NullPointerException, PropertyValueException, DataAccessException {

        String column = "name";
        Role role = roleService.findByProperty(column, "普通用户");

        User user = new UserBuild()
                .setSerialNumber(serialNumber)
                .setPassword(password)
                .setName(userName)
                .setBasicWage(4700)
                .setCreateTime()
                .setRole(role)
                .build();

        Object result = userService.registered(user);
        logger.info(result + " 用户执行了注册操作成功");
        return new ResultMap().resultSuccess().send();

    }

    /**
     * 添加用户接口
     * <p>
     * 管理员接口
     *
     * @param serialNumber
     * @param password
     * @param userName
     * @param salary
     * @return
     * @throws NullPointerException
     * @throws PropertyValueException
     * @throws DataAccessException
     */
    @RequestMapping(value = "/addUser", method = RequestMethod.POST)
    @AuthPassport
    public Map<String, Object> addUser(@RequestParam("serialNumber") String serialNumber,
                                       @RequestParam("password") String password,
                                       @RequestParam("userName") String userName,
                                       @RequestParam("salary") double salary)
            throws NullPointerException, PropertyValueException, DataAccessException {

        String column = "name";
        Role role = roleService.findByProperty(column, "普通用户");

        User user = new UserBuild()
                .setSerialNumber(serialNumber)
                .setPassword(password)
                .setName(userName)
                .setBasicWage(salary)
                .setCreateTime()
                .setRole(role)
                .build();

        Object result = userService.registered(user);
        logger.info(" 管理员添加了用户" + result);
        return new ResultMap().resultSuccess().send();
    }


    /**
     * 显示用户的个人信息
     * <p>
     * 管理员接口
     *
     * @param session 用户进程
     * @return 返回用户个人信息
     * @throws AuthorizationException 未登录异常
     */
    @RequestMapping(value = "/information", method = RequestMethod.POST)
    @AuthPassport
    @ResponseBody
    public Map<String, Object> information(HttpSession session) throws AuthorizationException, NullPointerException {

        User user = (User) session.getAttribute(SessionKey.USER);

        ResultDto resultDto = userInformation.resultDto(user);

        logger.info("查看了用户" + user.getId() + "的个人信息");

        return new ResultMap()
                .resultSuccess()
                .appendParameter(ResultMap.DATA, resultDto)
                .send();
    }

    /**
     * 获取用户的加班信息条数
     * <p>
     * 管理员接口
     *
     * @param limit       每页多少条
     * @param currentPage 当前页
     * @param session     与用户会话进程
     * @return 返回查询结果
     * @throws AuthorizationException 身份过期
     */
    @RequestMapping(value = "/personalOvertime", method = RequestMethod.POST)
    @AuthPassport
    @ResponseBody
    public Map<String, Object> personalOvertime(
            @RequestParam("limit") int limit,
            @RequestParam("currentPage") int currentPage,
            HttpSession session) throws AuthorizationException, IdentityOverdueException, NullPointerException {

        User user = (User) session.getAttribute(SessionKey.USER);

        ResultDto resultDto = presonalOvertimeInformation
                .setLimit(limit)
                .setCurrentPage(currentPage)
                .resultDto(user);

        logger.info("查看了用户" + user.getId() + "的个人加班信息");

        return new ResultMap()
                .appendParameter(ResultMap.DATA, resultDto)
                .resultSuccess()
                .send();
    }

    /**
     * 查询用户列表
     * <p>
     * 管理员接口
     *
     * @param limit
     * @param currentPage
     * @param session
     * @return
     */
    @RequestMapping(value = "/userList", method = RequestMethod.POST)
    @AuthPassport
    public Map<String, Object> userList(@RequestParam("limit") int limit,
                                        @RequestParam("currentPage") int currentPage,
                                        HttpSession session) {

        User user = (User) session.getAttribute(SessionKey.USER);

        if (SystemUtil.parameterNull(user)) {
            throw new IdentityOverdueException();
        }

        user = userService.findById(user.getId());

        if (!user.getRole().getName().equals("管理员")) {
            throw new AuthorizationException();
        }

        ResultDto resultDto = userListDTO
                .setLimit(limit)
                .setCurrentPage(currentPage)
                .setDesc(false)
                .resultDto();

        return new ResultMap()
                .resultSuccess()
                .appendParameter(ResultMap.DATA, resultDto)
                .send();
    }

    /**
     * 查看用户详细信息
     * <p>
     * 管理员、用户接口
     *
     * @return
     */
    @RequestMapping(value = "/userDetail", method = RequestMethod.POST)
    @AuthPassport
    public Map<String, Object> userDetail(@RequestParam("userId") int userId) {


        User user = userService.findById(userId);

        if (SystemUtil.parameterNull(user)) {
            throw new SelectException();
        }

        ResultDto resultDto = userDetailDTO.resultDto(user);

        logger.info("查看了用户" + user.getId() + "的详细个人信息");

        return new ResultMap()
                .resultSuccess()
                .appendParameter(ResultMap.DATA, resultDto)
                .send();

    }

    /**
     * 管理员更新用户基础信息
     * <p>
     * 管理员接口
     *
     * @param userId       需要修改用户的id
     * @param name         用户更新的姓名
     * @param serialNumber 用户警号
     * @param password     用户密码
     * @param salary       用户工资
     * @param session      用户进程
     * @return 更新结果
     */
    @RequestMapping(value = "/updateUser", method = RequestMethod.POST)
    @AuthPassport
    public Map<String, Object> updateUser(@RequestParam("userId") int userId,
                                          @RequestParam("name") String name,
                                          @RequestParam("serialNumber") String serialNumber,
                                          @RequestParam("password") String password,
                                          @RequestParam("salary") double salary,
                                          HttpSession session) {

        String isUpdataPassword = "d41d8cd98f00b204e9800998ecf8427e";

        User user = (User) session.getAttribute(SessionKey.USER);
        if (SystemUtil.parameterNull(user)) {
            throw new IdentityOverdueException();
        }

        //查询当前用户是否为管理
        user = userService.loadById(user.getId());

        Role role = user.getRole();

        if (!role.getName().equals("管理员")) {
            throw new AuthorizationException();
        }

        //获取需要更新的用户信息
        user = userService.findById(userId);

        if (SystemUtil.parameterNull(user)) {
            throw new SelectException();
        }

        if (password.equals(isUpdataPassword)) {
            user.setPassword(password);
        }

        user.setName(name);
        user.setSerialNumber(serialNumber);
        user.setBasicWage(salary);
        userService.update(user);
        logger.info("为用户" + user.getId() + "修改信息成功");

        return new ResultMap()
                .resultSuccess()
                .send();
    }

    /**
     * 用户注销
     * <p>
     * 管理员接口
     *
     * @param userId 用户id
     * @return 操作结果
     */
    @RequestMapping(value = "/deleteUser", method = RequestMethod.POST)
    @AuthPassport
    public Map<String, Object> deleteUser(@RequestParam("userId") int userId) {

        User user = userService.findById(userId);

        if (SystemUtil.parameterNull(user)) {
            throw new SelectException();
        }

        userService.deleteUser(user);
        logger.info("删除了用户" + user.getName() + "的信息");
        return new ResultMap().resultSuccess().send();
    }

    /**
     * 获取所有URL并查看
     * <p>
     * 管理员接口
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/getAllUrl", method = RequestMethod.POST)
    @AuthPassport
    public Set<String> getAllUrl(HttpServletRequest request) {
        Set<String> result = new HashSet<String>();
        WebApplicationContext wc = (WebApplicationContext) request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        RequestMappingHandlerMapping bean = wc.getBean(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = bean.getHandlerMethods();
        for (RequestMappingInfo rmi : handlerMethods.keySet()) {
            PatternsRequestCondition pc = rmi.getPatternsCondition();
            Set<String> pSet = pc.getPatterns();
            result.addAll(pSet);
        }

        return result;
    }

    /**
     * 将路径添加进入数据库
     * <p>
     * 管理员接口
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/saveAllUrl", method = RequestMethod.POST)
    @AuthPassport
    public Map<String, Object> saveAllUrl(HttpServletRequest request) {


        resourcesService.batchSaveResources(getAllUrl(request));

        return new ResultMap().resultSuccess().send();
    }



}
