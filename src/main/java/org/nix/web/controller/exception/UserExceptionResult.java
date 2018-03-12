package org.nix.web.controller.exception;

import org.apache.log4j.Logger;
import org.nix.exception.AccountNumberException;
import org.nix.exception.IdentityOverdueException;
import org.nix.exception.LuoErrorCode;
import org.nix.web.controller.utils.ResultMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Create by zhangpe0312@qq.com on 2018/3/10.
 *
 * 用户异常处理类
 */
@RestController
@RequestMapping(value = "/exception/user")
public class UserExceptionResult{
    //日志记录
    private static Logger logger = Logger.getLogger(UserExceptionResult.class);

    /**
     * 账号密码不匹配异常
     * @return 返回错误通知信息
     */
    @RequestMapping(value = "/accountNumberException")
    public Map<String, Object> accountNumberException() {

        return new ResultMap()
                .setResult(LuoErrorCode.USERNAME_PASSWORD_ERROR.getValue())
                .appendParameter(LuoErrorCode.USERNAME_PASSWORD_ERROR.getValue()
                        , LuoErrorCode.USERNAME_PASSWORD_ERROR.getDesc())
                .send();
    }

    /**
     * 身份过期异常
     * @return
     */
    @RequestMapping(value = "/identityOverdueException")
    public Map<String,Object> identityOverdueException(){
        return new ResultMap()
                .setResult(LuoErrorCode.IDENTITY_OVERDUE.getValue())
                .appendParameter(LuoErrorCode.IDENTITY_OVERDUE.getValue(),LuoErrorCode.IDENTITY_OVERDUE.getDesc())
                .send();
    }
}
