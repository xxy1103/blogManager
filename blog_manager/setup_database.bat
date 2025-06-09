@echo off
echo ====================================
echo 博客管理系统数据库设置脚本
echo ====================================
echo.

REM 检查MySQL是否安装
mysql --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到MySQL命令行工具，请确保MySQL已正确安装并添加到PATH环境变量中
    echo.
    echo 下载地址: https://dev.mysql.com/downloads/mysql/
    pause
    exit /b 1
)

echo [信息] 检测到MySQL已安装
echo.

REM 提示用户输入数据库连接信息
set /p DB_USER="请输入MySQL用户名 (默认: root): " 
if "%DB_USER%"=="" set DB_USER=root

set /p DB_PASSWORD="请输入MySQL密码: " 
if "%DB_PASSWORD%"=="" (
    echo [错误] 密码不能为空
    pause
    exit /b 1
)

set /p DB_HOST="请输入MySQL主机地址 (默认: localhost): "
if "%DB_HOST%"=="" set DB_HOST=localhost

set /p DB_PORT="请输入MySQL端口 (默认: 3306): "
if "%DB_PORT%"=="" set DB_PORT=3306

echo.
echo ====================================
echo 数据库连接配置:
echo 主机: %DB_HOST%
echo 端口: %DB_PORT%
echo 用户: %DB_USER%
echo ====================================
echo.

REM 测试数据库连接
echo [信息] 测试数据库连接...
mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo [错误] 数据库连接失败，请检查连接信息
    pause
    exit /b 1
)

echo [成功] 数据库连接测试通过
echo.

REM 执行数据库创建脚本
echo [信息] 开始创建数据库和表结构...
mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% < database_setup.sql
if errorlevel 1 (
    echo [错误] 数据库创建失败
    pause
    exit /b 1
)

echo [成功] 数据库创建完成！
echo.

REM 更新application.properties配置文件
echo [信息] 更新应用配置文件...
set APP_PROPS=src\main\resources\application.properties
set TEMP_PROPS=%APP_PROPS%.temp

REM 备份原配置文件
copy "%APP_PROPS%" "%APP_PROPS%.backup" >nul 2>&1

REM 更新数据库连接配置
(
for /f "usebackq delims=" %%i in ("%APP_PROPS%") do (
    set "line=%%i"
    setlocal enabledelayedexpansion
    if "!line:~0,24!"=="spring.datasource.url=" (
        echo spring.datasource.url=jdbc:mysql://%DB_HOST%:%DB_PORT%/blog_manager?useSSL=false^&serverTimezone=UTC^&allowPublicKeyRetrieval=true
    ) else if "!line:~0,29!"=="spring.datasource.username=" (
        echo spring.datasource.username=%DB_USER%
    ) else if "!line:~0,29!"=="spring.datasource.password=" (
        echo spring.datasource.password=%DB_PASSWORD%
    ) else (
        echo !line!
    )
    endlocal
)
) > "%TEMP_PROPS%"

move "%TEMP_PROPS%" "%APP_PROPS%" >nul 2>&1

echo [成功] 配置文件更新完成
echo.

echo ====================================
echo 数据库设置完成！
echo ====================================
echo.
echo 默认管理员账户:
echo 用户名: admin
echo 密码: admin123
echo 邮箱: admin@blogmanager.com
echo.
echo 接下来可以启动应用程序:
echo mvn spring-boot:run
echo.
echo 或者使用IDE运行 BlogManagerApplication.java
echo.

pause
