# E-Learning Lab 网络学习平台

`E-Learning Lab` 是一个前后端分离的在线学习平台，围绕课程学习、章节进度、普通练习、在线 Java 编程判题、算法可视化实验和后台内容管理构建。项目提供完整的用户端与管理端流程，适合本地部署、课程演示、教学辅助和二次开发。

## 项目架构

- 前端采用 Vue 3 + Vite 构建单页应用，负责用户学习端和管理员后台页面。
- 后端采用 Spring Boot 提供 REST API，负责鉴权、课程、章节、题库、判题、学习进度和文件上传。
- 数据库使用 MySQL 存储用户、课程、章节、题目、测试用例、答题记录和学习进度。
- 上传文件默认保存在项目根目录 `uploads/`，通过后端静态资源映射访问。
- 开发环境下 Vite 已配置 `/api` 和 `/uploads` 代理，便于前后端联调。

## 技术栈

### 后端

- Spring Boot 2.7
- MyBatis-Plus
- Sa-Token
- MySQL
- Lombok
- Hutool
- Maven
- Java Compiler API（在线 Java 编译与运行）

### 前端

- Vue 3
- Vite
- Vue Router
- Axios
- CSS3 响应式布局

### 可选能力

- `ffmpeg`：用于上传视频自动转码为浏览器兼容性更好的 MP4/H.264 格式。

## 系统环境

- JDK 8+，推荐 JDK 17。
- MySQL 8.x。
- Maven 3.8+。
- Node.js 18+。
- npm 9+。
- 可选安装 `ffmpeg`，并加入系统 PATH。

## 功能模块

### 账号与权限

- 用户注册、登录、退出。
- 管理员与普通用户角色隔离。
- 前端路由守卫保护管理端页面。
- 后端会话失效后，前端自动清理登录态并跳转登录页。

### 用户端

- 课程列表：展示课程分类、难度、简介和统一比例课程封面。
- 课程详情：展示课程信息、章节列表和章节学习进度。
- 章节学习：支持文章、视频、普通练习入口、编程题入口和算法实验入口。
- 普通练习：支持单选题和判断题，提交后展示得分、参考答案、用户答案与解析。
- 在线编程：支持 Java 代码编辑、模板恢复、缩进整理、运行判题和友好编译错误提示。
- 算法实验室：支持冒泡排序、选择排序、插入排序、二分查找和线性查找的可视化演示。
- 学习进度：展示章节学习状态、练习完成情况和分数统计。

### 管理端

- 管理首页：展示平台资源数据、内容配置流程和常用管理入口。
- 课程管理：新增、编辑、发布/下架课程，支持上传课程封面。
- 章节管理：维护章节内容，支持文章、视频、普通练习、编程题和算法实验等类型。
- 普通题管理：维护单选题和判断题，单选题通过勾选正确选项确定答案，判断题通过“正确/错误”点选确定答案。
- 编程题管理：维护题目描述、模板代码、参考答案和测试用例。
- 学习进度统计：查看用户学习和练习完成情况。
- 文件上传：支持课程封面和章节视频上传，视频可尝试自动转码。

## 目录结构

```text
E-Learning
├── backend
│   ├── sql
│   │   ├── schema.sql
│   │   └── data.sql
│   └── src/main
│       ├── java/com/elearning/backend
│       └── resources/application.yml
├── frontend
│   ├── src
│   │   ├── views
│   │   ├── api.js
│   │   ├── router.js
│   │   └── styles.css
│   └── vite.config.js
├── uploads
└── README.md
```

说明：

- `backend/sql/schema.sql`：数据库建表脚本。
- `backend/sql/data.sql`：初始化演示数据，建议空库执行一次。
- `uploads/`：本地上传文件目录，已在 `.gitignore` 中忽略。
- `PROJECT_CODING_GUIDE.md`：本地开发说明文件，已在 `.gitignore` 中忽略。

## 本地运行

### 1. 准备数据库

创建数据库：

```sql
CREATE DATABASE e_learning DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

按顺序执行：

```text
backend/sql/schema.sql
backend/sql/data.sql
```

### 2. 启动后端

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息。

进入后端目录：

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

### 3. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

## 初始化账号

管理员账号写在 `backend/sql/data.sql` 中。

```text
用户名：admin
密码：admin123
```

普通用户可以通过注册页面创建，也可以在初始化数据中查看示例用户。

## 文件上传与视频转码

- 上传目录：项目根目录下的 `uploads/`。
- 访问路径：`/uploads/**`。
- 支持课程封面、章节视频等文件上传。
- 支持常见视频格式上传，例如 `mp4`、`webm`、`mov`、`mkv`、`avi`、`flv` 等。
- 如果本机安装了 `ffmpeg`，上传视频后会尝试自动转码为 MP4/H.264。
- 如果未安装 `ffmpeg`，系统仍会保留原始视频文件，但浏览器播放兼容性取决于原视频编码。

## 常用命令

前端构建：

```bash
cd frontend
npm run build
```

后端编译：

```bash
cd backend
mvn -DskipTests compile
```

## 设计特点

- 页面采用米白背景、墨黑文字和朱砂红强调色。
- 首页、课程页和管理端使用卡片化布局，重点信息更集中。
- 普通题、编程题和算法实验均提供完整的学习闭环。
- 管理端提供流程说明，降低课程、章节、题目维护成本。
- 代码结构清晰，便于扩展更多题型、更多算法或其他编程语言判题。
