package org.sj.iot.service.tx.impl;

import org.sj.iot.dao.IUserDAO;
import org.sj.iot.service.tx.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户业务操作接口实现类
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@Service
public class UserServiceImpl implements IUserService {
    @Autowired
    private IUserDAO userDAO;

    public UserServiceImpl() {
        System.err.println("");
    }
}
