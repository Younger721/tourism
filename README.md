# 旅游管理系统

这是一个前后端分离的毕业设计级旅游管理系统，包含用户端、管理员端、AI 双模式行程推荐和灵感足迹地图。

## 技术栈

- 后端：Java 17、Spring Boot 3.2、MyBatis-Plus、MySQL 8、阿里通义千问 DashScope SDK
- 前端：Vue 3、Vite、Element Plus、ECharts、Axios

## 启动步骤

1. 创建数据库并导入脚本：

```sql
source database/schema.sql;
```

2. 修改后端数据库配置：

```yaml
backend/src/main/resources/application.yaml
```

3. 启动后端：

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd backend
mvn spring-boot:run
```

4. 启动前端：

```powershell
cd frontend
npm install
npm run dev
```

## 默认账号

- 管理员：admin / admin123
- 普通用户：user / 123456

## AI 配置

如果需要真实调用通义千问，启动后端前设置：

```powershell
$env:DASHSCOPE_API_KEY='你的API Key'
$env:DASHSCOPE_MODEL='qwen-turbo'
```

如果没有设置 API Key，系统会自动生成演示行程，方便先完成项目演示。

## MinIO 图片上传配置

项目通过 MinIO 保存景点、线路、酒店和灵感足迹图片。默认 bucket 为 `tourism`。

```powershell
$env:MINIO_ENDPOINT='http://你的虚拟机IP:9000'
$env:MINIO_PUBLIC_URL='http://你的虚拟机IP:9000/tourism'
$env:MINIO_ACCESS_KEY='minioadmin'
$env:MINIO_SECRET_KEY='minioadmin'
$env:MINIO_BUCKET='tourism'
```

如果 bucket 是公开读取，上传接口返回的图片地址可以直接在浏览器访问。
