CREATE DATABASE IF NOT EXISTS tourism_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tourism_management;

DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS friendship;
DROP TABLE IF EXISTS friend_request;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS footprint;
DROP TABLE IF EXISTS travel_post;
DROP TABLE IF EXISTS ai_trip_plan;
DROP TABLE IF EXISTS scenic_spot;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录用户名',
  password VARCHAR(100) NOT NULL COMMENT '登录密码，毕业设计阶段明文保存，正式项目应加密',
  nickname VARCHAR(50) COMMENT '用户昵称',
  phone VARCHAR(30) COMMENT '手机号',
  avatar_url VARCHAR(500) COMMENT '头像图片地址',
  bio VARCHAR(500) COMMENT '个人简介',
  city VARCHAR(50) COMMENT '所在城市',
  role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER普通用户，ADMIN管理员',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1正常，0禁用',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='系统用户表，保存普通用户和管理员账号';

CREATE TABLE scenic_spot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '景点ID',
  name VARCHAR(100) NOT NULL COMMENT '景点名称',
  province VARCHAR(50) COMMENT '所在省份',
  city VARCHAR(50) COMMENT '所在城市',
  address VARCHAR(255) COMMENT '详细地址',
  level VARCHAR(20) COMMENT '景区等级，例如5A、4A',
  price DECIMAL(10, 2) DEFAULT 0 COMMENT '参考价格，仅作为信息展示，不用于交易',
  image_url VARCHAR(500) COMMENT '景点图片地址',
  description TEXT COMMENT '景点介绍',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '展示状态：1展示，0隐藏',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='景点灵感表，用于展示目的地资料和辅助AI推荐';

CREATE TABLE travel_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '旅游记录ID',
  user_id BIGINT NOT NULL COMMENT '发布用户ID',
  title VARCHAR(120) NOT NULL COMMENT '记录标题',
  content TEXT NOT NULL COMMENT '记录正文',
  destination VARCHAR(100) COMMENT '旅行目的地',
  province_code VARCHAR(20) COMMENT '省份编码，用于地图关联',
  province_name VARCHAR(50) COMMENT '省份名称',
  image_url VARCHAR(500) COMMENT '封面图片地址',
  travel_date DATE COMMENT '旅行日期',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT '可见范围：PUBLIC公开',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='旅游记录表，类似博客动态，用户发布旅行照片和文字';

CREATE TABLE ai_trip_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'AI推荐记录ID',
  user_id BIGINT NOT NULL COMMENT '发起推荐的用户ID',
  mode VARCHAR(40) NOT NULL COMMENT '推荐模式：DESTINATION_PLAN指定目的地，AI_DESTINATION_PLAN由AI推荐目的地',
  destination VARCHAR(100) COMMENT '用户指定的目的地',
  recommended_destination VARCHAR(100) COMMENT 'AI推荐的目的地',
  people_count INT COMMENT '出行人数',
  days INT COMMENT '出游天数',
  budget DECIMAL(10, 2) COMMENT '旅行预算',
  requirements TEXT COMMENT '用户输入的旅游要求',
  start_date DATE COMMENT '计划出发日期',
  result_json JSON COMMENT 'AI生成的完整行程JSON',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='AI行程推荐记录表，保存用户输入条件和AI生成结果';

CREATE TABLE footprint (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '足迹ID',
  user_id BIGINT NOT NULL COMMENT '所属用户ID',
  province_code VARCHAR(20) NOT NULL COMMENT '省份编码',
  province_name VARCHAR(50) NOT NULL COMMENT '省份名称',
  title VARCHAR(100) NOT NULL COMMENT '足迹标题',
  content TEXT COMMENT '足迹文字记录',
  image_url VARCHAR(500) COMMENT '足迹照片地址',
  travel_date DATE COMMENT '旅行日期',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='灵感足迹表，保存用户在中国地图各省份下的照片和文字';

CREATE TABLE comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
  user_id BIGINT NOT NULL COMMENT '评论用户ID',
  target_type VARCHAR(30) NOT NULL COMMENT '评论对象类型，例如POST、SCENIC',
  target_id BIGINT NOT NULL COMMENT '评论对象ID',
  rating INT DEFAULT 5 COMMENT '评分，默认5分',
  content TEXT COMMENT '评论内容',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='评论表，保存用户对旅游记录或景点灵感的评论';

CREATE TABLE favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
  user_id BIGINT NOT NULL COMMENT '收藏用户ID',
  target_type VARCHAR(30) NOT NULL COMMENT '收藏对象类型，例如POST、SCENIC',
  target_id BIGINT NOT NULL COMMENT '收藏对象ID',
  target_name VARCHAR(100) COMMENT '收藏对象名称，便于列表展示',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='收藏表，保存用户收藏的旅游记录或景点灵感';

CREATE TABLE friend_request (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友申请ID',
  from_user_id BIGINT NOT NULL COMMENT '申请人用户ID',
  to_user_id BIGINT NOT NULL COMMENT '接收人用户ID',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态：PENDING待处理，ACCEPTED已同意，REJECTED已拒绝',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='好友申请表，保存用户之间的加好友请求';

CREATE TABLE friendship (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  friend_id BIGINT NOT NULL COMMENT '好友用户ID',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间',
  UNIQUE KEY uk_friendship_pair (user_id, friend_id)
) COMMENT='好友关系表，双向好友会保存两条记录，便于查询我的好友列表';

CREATE TABLE chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '聊天消息ID',
  sender_id BIGINT NOT NULL COMMENT '发送人用户ID',
  receiver_id BIGINT NOT NULL COMMENT '接收人用户ID',
  content TEXT NOT NULL COMMENT '消息内容',
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '阅读状态：0未读，1已读',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '更新时间'
) COMMENT='聊天消息表，保存好友之间的实时聊天历史';

INSERT INTO sys_user(username, password, nickname, phone, avatar_url, bio, city, role, status, create_time, update_time)
VALUES
  ('admin', 'admin123', '系统管理员', '13800000000', NULL, '负责维护社区内容和景点灵感。', '杭州', 'ADMIN', 1, NOW(), NOW()),
  ('user', '123456', '演示用户', '13900000000', NULL, '喜欢记录旅行灵感。', '上海', 'USER', 1, NOW(), NOW()),
  ('traveler', '123456', '山海旅人', '13700000000', NULL, '偏爱山海、古城和慢旅行。', '成都', 'USER', 1, NOW(), NOW());

INSERT INTO scenic_spot(name, province, city, address, level, price, image_url, description, status, create_time, update_time)
VALUES
  ('西湖风景名胜区', '浙江', '杭州', '杭州市西湖区龙井路1号', '5A', 0, 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80', '湖岸、断桥和茶山串成经典杭州半日线，适合第一次来杭州的人。', 1, NOW(), NOW()),
  ('鼓浪屿', '福建', '厦门', '厦门市思明区鼓浪屿街道', '5A', 0, 'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?auto=format&fit=crop&w=900&q=80', '小岛街巷、老别墅和海边步道适合慢走，傍晚光线很好。', 1, NOW(), NOW()),
  ('大唐不夜城', '陕西', '西安', '西安市雁塔区慈恩路46号', '4A', 0, 'https://images.unsplash.com/photo-1599571234909-29ed5d1321d6?auto=format&fit=crop&w=900&q=80', '夜间灯光、街头演艺和唐风建筑集中，适合安排晚饭后游玩。', 1, NOW(), NOW()),
  ('张家界国家森林公园', '湖南', '张家界', '张家界市武陵源区金鞭路', '5A', 227, 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=900&q=80', '峰林地貌很有冲击力，适合两到三天深度徒步和观景。', 1, NOW(), NOW()),
  ('丽江古城', '云南', '丽江', '丽江市古城区大研街道', '5A', 0, 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=900&q=80', '石板路、水渠和纳西院落保留了古城气质，夜晚更热闹。', 1, NOW(), NOW()),
  ('成都宽窄巷子', '四川', '成都', '成都市青羊区长顺上街', '2A', 0, 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=900&q=80', '茶馆、小吃、院落和文创店集中，适合半日城市休闲。', 1, NOW(), NOW()),
  ('桂林漓江', '广西', '桂林', '桂林市灵川县至阳朔县水域', '5A', 210, 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=900&q=80', '山水倒影和江面航线很经典，推荐晴天上午出发。', 1, NOW(), NOW()),
  ('青岛栈桥', '山东', '青岛', '青岛市市南区太平路14号', '4A', 0, 'https://images.unsplash.com/photo-1471922694854-ff1b63b20054?auto=format&fit=crop&w=900&q=80', '老城海岸线、回澜阁和红瓦街区适合串成轻松步行路线。', 1, NOW(), NOW()),
  ('哈尔滨中央大街', '黑龙江', '哈尔滨', '哈尔滨市道里区中央大街', '4A', 0, 'https://images.unsplash.com/photo-1483664852095-d6cc6870702d?auto=format&fit=crop&w=900&q=80', '欧式建筑、面包石路和冰雪季氛围浓厚，适合冬天夜游。', 1, NOW(), NOW());

INSERT INTO travel_post(user_id, title, content, destination, province_code, province_name, image_url, travel_date, visibility, create_time, update_time)
VALUES
  (2, '杭州两日慢旅行', '第一天沿西湖走苏堤和白堤，第二天去龙井村喝茶。整趟行程不赶，适合想放慢节奏的人。', '杭州', '330000', '浙江', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (3, '厦门看海记录', '上午逛沙坡尾，下午上鼓浪屿，傍晚回到海边等日落。厦门适合把吃饭和散步穿插安排。', '厦门', '350000', '福建', 'https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (2, '深圳周末看海', '从广州高铁到深圳很快，下午骑行深圳湾，晚上去后海吃饭。第二天可以去盐田看海。', '深圳', '440000', '广东', 'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (3, '西安夜游记录', '白天看碑林和城墙，晚上去大唐不夜城。西安的夜景和小吃很适合一起安排。', '西安', '610000', '陕西', 'https://images.unsplash.com/photo-1599571234909-29ed5d1321d6?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (2, '成都慢生活两日', '人民公园喝茶、宽窄巷子闲逛、晚上吃火锅。成都不用排太满，留时间坐下来最舒服。', '成都', '510000', '四川', 'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (3, '桂林山水打卡', '上午坐船看漓江，两岸山形变化很多；下午去阳朔西街吃饭，晚上住在遇龙河附近。', '桂林', '450000', '广西', 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (2, '青岛海边散步', '从栈桥走到八大关，沿路看海、拍红瓦建筑，傍晚去海边吃海鲜。一天路线很顺。', '青岛', '370000', '山东', 'https://images.unsplash.com/photo-1471922694854-ff1b63b20054?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW()),
  (3, '丽江古城夜色', '下午进古城慢慢逛，夜里灯光亮起来后更有氛围。第二天可以去束河，安静一些。', '丽江', '530000', '云南', 'https://images.unsplash.com/photo-1518005020951-eccb494ad742?auto=format&fit=crop&w=900&q=80', CURDATE(), 'PUBLIC', NOW(), NOW());

INSERT INTO friendship(user_id, friend_id, create_time, update_time)
VALUES
  (2, 3, NOW(), NOW()),
  (3, 2, NOW(), NOW());
