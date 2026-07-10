package com.orientsec.idap.core.config;

/** Bean names and property keys retained from the intranet IDAP base. */
public class ConfigConstant {

    public static final String Data_Source_NAME = "idapDataSource";
    public static final String Data_Source_Properties = "idapDataSourceProperties";
    public static final String SqlSession_Factory_NAME = "idapSqlSessionFactory";
    public static final String Transaction_Manager = "idapTransactionManager";
    public static final String SqlSession_Template = "idapSqlSessionTemplate";
    public static final String MapperScannerConfigurer = "idapMapperScannerConfigurer";
    public static final String JdbcTemplate = "idapJdbcTemplate";
    public static final String Data_Source_ConfigPrefix = "datasource.idap";
    public static final String Data_Source_MapperLoc = "${datasource.idap.mapperlocation}";
}
