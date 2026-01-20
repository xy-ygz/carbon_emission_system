package com.bjfu.carbon.controller;


import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bjfu.carbon.annotation.RateLimit;
import com.bjfu.carbon.common.ErrorCode;
import com.bjfu.carbon.common.ResultUtils;
import com.bjfu.carbon.domain.User;
import com.bjfu.carbon.security.JwtTokenProvider;
import com.bjfu.carbon.service.UserService;
import com.bjfu.carbon.utils.ExcelUtils;
import com.bjfu.carbon.utils.SaltUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用户 控制层
 *
 * @author xgy
 * @since 2023-02-13
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;


    /**
     * 批量注册
     * 通过excel导入(姓名，账号(不允许重复)，部门，手机号)实现批量注册
     * 传入的xlsx文件第一行必须为为表头(姓名，账号(不允许重复)，部门，手机号)
     * 密码统一初始化为123456
     * @param file xxx.xlsx文件
     * @return
     * @throws Exception
     */
    // todo 前端 批量导入用户按钮 && excel 模板
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/excelRegister")
    public Object excelRegister(MultipartFile file) throws Exception {
        if (file==null || file.isEmpty()){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"文件为空，请选择文件！");
        }
        String filename = file.getOriginalFilename();
        assert filename != null;
        if (!filename.endsWith(".xlsx")){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"文件格式错误！");
        }
        // 获取Excel中的数据
        List<Map<String,String>> list = ExcelUtils.excelToShopIdList(file.getInputStream());
        if(list.isEmpty())
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"文件数据为空，请录入数据！");

        if(!list.get(0).get("col0").equals("姓名") ||!list.get(0).get("col1").equals("账号") || !list.get(0).get("col2").equals("部门") || !list.get(0).get("col3").equals("手机号"))
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"xlsx表头不对，请将录入数据列表头设为(姓名，账号，部门，手机号)!");

        String regex="[0-9]+";  //手机号码纯数字
        for(int i=1;i<list.size();i++){
            Map<String, String> map = list.get(i);
            String phone = map.get("col3");
            Matcher m= Pattern.compile(regex).matcher(phone);

            if(phone.length()!=11){
                return ResultUtils.error(ErrorCode.PARAMS_ERROR,"插入失败，手机号长度录入有误！");
            }
            if(!m.matches()){
                return ResultUtils.error(ErrorCode.PARAMS_ERROR,"插入失败，手机号包含其他字符！");
            }
        }

        //批量注册
        List<User> userList = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for(int i=1;i<list.size();i++){
            Map<String, String> map = list.get(i);
            String name = String.valueOf(map.get("col0"));
            String username = String.valueOf(map.get("col1")) ;
            String department = String.valueOf(map.get("col2"));
            String phone = String.valueOf(map.get("col3"));

            set.add(username);

            User u = new User();
            String salt = SaltUtils.getSalt(4);
            u.setSalt(salt);
            u.setName(name);
            u.setUsername(username);
            u.setPassword(SecureUtil.md5(salt + "123456")); //初始密码统一为:123456
            u.setDepartment(department);
            u.setPhone(phone);

            userList.add(u);
        }
        if(set.size()<userList.size()){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"录入数据中存在用户名重复！");
        }
        boolean flag = userService.saveBatch(userList);
        if(flag)
            return ResultUtils.success("批量注册成功！");
        else
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"插入失败，请检查录入数据！");
    }

    /**
     * 用户登录
     * @param user
     * @return
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/login")
    public Object login(@RequestBody User user){
        String username = user.getUsername();
        String password = user.getPassword();

        if (StringUtils.isAnyBlank(username, password)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"账号或密码输入为空！");
        }
        LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
        lqw.eq(User::getUsername,username);

        User u = userService.getOne(lqw);
        if(Objects.isNull(u)){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"该账户用户不存在！");
        }

        // 验证密码（兼容明文和MD5+盐值）
        boolean passwordMatches = password.equals(u.getPassword()) || 
                u.getPassword().equals(SecureUtil.md5(u.getSalt() + password));
        
        if (passwordMatches) {
            // 生成JWT Token和Refresh Token
            String token = jwtTokenProvider.generateToken(u.getUsername(), u.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(u.getUsername(), u.getId());
            
            // 返回用户信息和Token
            Map<String, Object> result = new HashMap<>();
            result.put("id", u.getId());
            result.put("name", u.getName());
            result.put("username", u.getUsername());
            result.put("type", u.getStatus()); // status: 0后勤人员/1管理员
            result.put("token", token);
            result.put("refreshToken", refreshToken);
            
            return ResultUtils.success(result);
        } else {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "账号或密码错误！");
        }
    }

    /**
     * 刷新Token
     * @param requestBody 包含refreshToken的请求体
     * @return 新的Token和Refresh Token
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/refreshToken")
    public Object refreshToken(@RequestBody Map<String, String> requestBody) {
        String refreshToken = requestBody.get("refreshToken");
        
        if (StringUtils.isBlank(refreshToken)) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "刷新令牌不能为空！");
        }
        
        try {
            // 验证Refresh Token有效性
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            if (!jwtTokenProvider.validateToken(refreshToken, username)) {
                return ResultUtils.error(ErrorCode.NOT_LOGIN, "刷新令牌无效或已过期！");
            }
            
            // 从Refresh Token中获取用户ID
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            
            // 生成新的Token和Refresh Token
            String newToken = jwtTokenProvider.generateToken(username, userId);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username, userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", newToken);
            result.put("refreshToken", newRefreshToken);
            
            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("刷新Token失败: {}", e.getMessage());
            return ResultUtils.error(ErrorCode.NOT_LOGIN, "刷新令牌无效！");
        }
    }

    /**
     * 退出登录
     * @return 退出结果
     */
    @RateLimit(ipLimit = 5, apiLimit = 100)
    @PostMapping("/logout")
    public Object logout() {
        // 后端不需要处理token存储，只需要返回成功即可
        // 前端会负责清除所有token
        return ResultUtils.success("退出登录成功！");
    }
}
