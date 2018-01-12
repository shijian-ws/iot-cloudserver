package org.sj.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import org.sj.iot.model.User;

/**
 * 用户数据访问接口
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@Mapper
public interface IUserDAO {
    /**
     * 根据手机号查询用户
     */
    User getUserByMobile(String mobile);

    /**
     * 根据手机号和密码查询用户
     */
    User getUserByMobileAndPassword(String mobile, String password);

    /**
     * 根据电邮查询用户
     */
    User getUserByEmail(String email);

    /**
     * 根据电邮和密码查询用户
     */
    User getUserByEmailAndPassword(String email, String password);

    /**
     * 根据用户名查询用户
     */
    User getUserByUsername(String username);

    /**
     * 根据用户名和密码查询用户
     */
    User getUserByUsernameAndPassword(String username, String password);

    /**
     * 检查用户名是否存在
     */
    boolean checkUsername(String username);

    /**
     * 创建用户
     */
    long saveUser(User user);

    /**
     * 更新用户
     */
    long updateUser(User user);

    /**
     * 移除用户手机
     */
    long removeMobile(String userId);

    /**
     * 移除用户电邮
     */
    long removeEmail(String userId);

    /**
     * 移除用户用户名
     */
    long removeUsername(String userId);
}
