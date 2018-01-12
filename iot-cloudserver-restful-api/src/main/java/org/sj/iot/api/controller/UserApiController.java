package org.sj.iot.api.controller;

import org.sj.iot.model.User;
import org.sj.iot.service.tx.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户控制
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@RestController
@RequestMapping("/user")
public class UserApiController {
    @Autowired
    private IUserService userService;

    @GetMapping("")
    public Object index(HttpServletRequest request) {
        throw new IllegalArgumentException("不支持操作!");
    }

    @PostMapping("/login")
    public Object login(@RequestBody User user, HttpServletRequest request) {
        return null;
    }
}
