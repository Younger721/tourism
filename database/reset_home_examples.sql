-- USE tourism_management;

-- SET @scenic_names = '西湖风景名胜区,鼓浪屿,大唐不夜城,张家界国家森林公园,丽江古城,成都宽窄巷子,桂林漓江,青岛栈桥,哈尔滨中央大街';
-- SET @post_titles = '杭州两日慢旅行,厦门看海记录,深圳周末看海,西安夜游记录,成都慢生活两日,桂林山水打卡,青岛海边散步,丽江古城夜色';

-- DELETE FROM favorite
-- WHERE (target_type = 'SCENIC' AND FIND_IN_SET(target_name, @scenic_names))
--    OR (target_type = 'POST' AND FIND_IN_SET(target_name, @post_titles));

-- DELETE f FROM favorite f
-- JOIN scenic_spot s ON f.target_type = 'SCENIC' AND f.target_id = s.id
-- WHERE FIND_IN_SET(s.name, @scenic_names);

-- DELETE f FROM favorite f
-- JOIN travel_post p ON f.target_type = 'POST' AND f.target_id = p.id
-- WHERE FIND_IN_SET(p.title, @post_titles);

-- DELETE c FROM comment c
-- JOIN scenic_spot s ON c.target_type = 'SCENIC' AND c.target_id = s.id
-- WHERE FIND_IN_SET(s.name, @scenic_names);

-- DELETE c FROM comment c
-- JOIN travel_post p ON c.target_type = 'POST' AND c.target_id = p.id
-- WHERE FIND_IN_SET(p.title, @post_titles);

-- DELETE FROM scenic_spot
-- WHERE FIND_IN_SET(name, @scenic_names);

-- DELETE FROM travel_post
-- WHERE FIND_IN_SET(title, @post_titles);

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
