package com.orientsec.idap.core.mapper.ext;

import com.orientsec.idap.core.mapper.IdapUserInfoMapper;
import org.springframework.context.annotation.Primary;

/** Extension point for custom user mapper SQL. */
@Primary
public interface IdapUserInfoMapperExt extends IdapUserInfoMapper {}
