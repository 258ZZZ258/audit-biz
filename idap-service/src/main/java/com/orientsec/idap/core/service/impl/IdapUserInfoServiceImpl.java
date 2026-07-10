package com.orientsec.idap.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.orientsec.idap.core.mapper.IdapUserInfoMapper;
import com.orientsec.idap.core.model.IdapUserInfo;
import com.orientsec.idap.core.service.IdapUserInfoService;
import org.springframework.stereotype.Service;

@Service
public class IdapUserInfoServiceImpl extends ServiceImpl<IdapUserInfoMapper, IdapUserInfo>
        implements IdapUserInfoService {}
