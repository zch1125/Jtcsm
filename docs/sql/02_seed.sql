-- ========================================================
-- 今天吃什么 · 种子数据
-- 包含：会员套餐、食材字典、30+ 道预置菜谱（含用料和步骤）
-- ========================================================
USE jtcsm;


-- ========================================================
-- 食材字典（~80种常见食材）
-- ========================================================
INSERT INTO ingredient (name, category) VALUES
-- 蔬菜
('番茄','蔬菜'),('土豆','蔬菜'),('鸡蛋','蔬菜'),('包菜','蔬菜'),
('西兰花','蔬菜'),('白菜','蔬菜'),('菠菜','蔬菜'),('生菜','蔬菜'),
('茄子','蔬菜'),('青椒','蔬菜'),('红椒','蔬菜'),('黄瓜','蔬菜'),
('胡萝卜','蔬菜'),('洋葱','蔬菜'),('大葱','蔬菜'),('姜','蔬菜'),
('蒜','蔬菜'),('蒜苗','蔬菜'),('豆芽','蔬菜'),('豆腐','蔬菜'),
('豆皮','蔬菜'),('香菇','蔬菜'),('金针菇','蔬菜'),('玉米','蔬菜'),
('冬瓜','蔬菜'),('南瓜','蔬菜'),
-- 肉类
('猪五花肉','肉类'),('猪里脊','肉类'),('排骨','肉类'),('猪瘦肉','肉类'),
('猪蹄','肉类'),('鸡腿','肉类'),('鸡胸肉','肉类'),('鸡翅','肉类'),
('鸡爪','肉类'),('鸭肉','肉类'),('牛肉','肉类'),('牛腩','肉类'),
('牛里脊','肉类'),('羊肉','肉类'),('腊肉','肉类'),('火腿','肉类'),
-- 水产海鲜
('鲈鱼','海鲜'),('带鱼','海鲜'),('草鱼','海鲜'),('鲫鱼','海鲜'),
('鲢鱼头','海鲜'),('虾','海鲜'),('蛤蜊','海鲜'),('鱿鱼','海鲜'),
('扇贝','海鲜'),('螃蟹','海鲜'),
-- 调料
('盐','调料'),('白糖','调料'),('生抽','调料'),('老抽','调料'),
('醋','调料'),('料酒','调料'),('蚝油','调料'),('豆瓣酱','调料'),
('番茄酱','调料'),('辣椒酱','调料'),('花椒','调料'),('干辣椒','调料'),
('八角','调料'),('桂皮','调料'),('香叶','调料'),('五香粉','调料'),
('胡椒粉','调料'),('淀粉','调料'),('食用油','调料'),('芝麻油','调料'),
('辣椒油','调料'),('蒜蓉','调料'),
-- 主食
('大米','主食'),('面条','主食'),('米粉','主食'),('面粉','主食'),
('河粉','主食'),('年糕','主食'),
-- 干货
('紫菜','干货'),('木耳','干货'),('粉丝','干货'),('花生','干货'),
('芝麻','干货'),('红枣','干货'),('枸杞','干货'),('虾皮','干货'),
-- 其他
('葱','蔬菜'),('可乐','饮料'),('饺子皮','主食'),('雪碧','饮料');

-- ========================================================
-- 菜谱数据（32 道经典家常菜）
-- ========================================================

-- 1. 番茄炒蛋
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('番茄炒蛋','经典国民家常菜，酸甜可口，老少皆宜','家常菜','简单','炒',15,120,0,0,1,'system');
SET @r1 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r1,(SELECT id FROM ingredient WHERE name='番茄'),'番茄','2个',1),
(@r1,(SELECT id FROM ingredient WHERE name='鸡蛋'),'鸡蛋','3个',2),
(@r1,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',3),
(@r1,(SELECT id FROM ingredient WHERE name='白糖'),'白糖','1勺',4),
(@r1,(SELECT id FROM ingredient WHERE name='葱'),'葱花','少许',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r1,1,'番茄洗净切块，鸡蛋打散加少许盐搅匀',2),
(@r1,2,'热锅倒油，倒入蛋液炒至凝固盛出',2),
(@r1,3,'锅中再加少许油，放入番茄块翻炒出汁',3),
(@r1,4,'加入白糖和盐调味，倒回炒好的鸡蛋翻炒均匀',2),
(@r1,5,'撒上葱花，出锅装盘',1);

-- 2. 红烧肉
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('红烧肉','色泽红亮，肥而不腻，入口即化的经典硬菜','家常菜','普通','炖',90,580,0,0,1,'system');
SET @r2 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r2,(SELECT id FROM ingredient WHERE name='猪五花肉'),'猪五花肉','500克',1),
(@r2,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',2),
(@r2,(SELECT id FROM ingredient WHERE name='老抽'),'老抽','1勺',3),
(@r2,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','2勺',4),
(@r2,(SELECT id FROM ingredient WHERE name='白糖'),'冰糖','30克',5),
(@r2,(SELECT id FROM ingredient WHERE name='姜'),'姜片','3片',6),
(@r2,(SELECT id FROM ingredient WHERE name='葱'),'葱段','2段',7),
(@r2,(SELECT id FROM ingredient WHERE name='八角'),'八角','2个',8);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r2,1,'五花肉切成3厘米方块，冷水下锅焯水去浮沫，捞出沥干',5),
(@r2,2,'锅中放少许油，小火炒化冰糖至枣红色',3),
(@r2,3,'放入肉块快速翻炒上色，加入姜片、葱段、八角爆香',3),
(@r2,4,'烹入料酒、生抽、老抽翻炒均匀',2),
(@r2,5,'加入没过肉块的开水，大火烧开转小火炖60分钟',60),
(@r2,6,'大火收汁至汤汁浓稠，出锅装盘',5);

-- 3. 麻婆豆腐
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('麻婆豆腐','麻辣鲜香，豆腐嫩滑入味，川菜代表之一','川菜','普通','烧',15,260,0,0,1,'system');
SET @r3 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r3,(SELECT id FROM ingredient WHERE name='豆腐'),'嫩豆腐','400克',1),
(@r3,(SELECT id FROM ingredient WHERE name='猪瘦肉'),'猪肉末','100克',2),
(@r3,(SELECT id FROM ingredient WHERE name='豆瓣酱'),'豆瓣酱','1大勺',3),
(@r3,(SELECT id FROM ingredient WHERE name='花椒'),'花椒粉','1小勺',4),
(@r3,(SELECT id FROM ingredient WHERE name='淀粉'),'水淀粉','适量',5),
(@r3,(SELECT id FROM ingredient WHERE name='葱'),'葱花','适量',6);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r3,1,'豆腐切2厘米方块，开水加盐焯烫2分钟捞出',3),
(@r3,2,'热锅倒油，炒散猪肉末至变色',2),
(@r3,3,'加入豆瓣酱炒出红油',2),
(@r3,4,'加适量清水烧开，轻轻放入豆腐块，小火煮3分钟',5),
(@r3,5,'淋入水淀粉勾芡，撒花椒粉和葱花出锅',3);

-- 4. 宫保鸡丁
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('宫保鸡丁','酸甜微辣，花生酥脆，经典川菜','川菜','普通','炒',25,380,0,0,1,'system');
SET @r4 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r4,(SELECT id FROM ingredient WHERE name='鸡胸肉'),'鸡胸肉','300克',1),
(@r4,(SELECT id FROM ingredient WHERE name='花生'),'花生米','50克',2),
(@r4,(SELECT id FROM ingredient WHERE name='干辣椒'),'干辣椒','10个',3),
(@r4,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',4),
(@r4,(SELECT id FROM ingredient WHERE name='醋'),'醋','1勺',5),
(@r4,(SELECT id FROM ingredient WHERE name='白糖'),'白糖','1勺',6),
(@r4,(SELECT id FROM ingredient WHERE name='淀粉'),'淀粉','适量',7),
(@r4,(SELECT id FROM ingredient WHERE name='葱'),'葱段','适量',8);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r4,1,'鸡胸肉切丁，加生抽、淀粉抓匀腌制15分钟',15),
(@r4,2,'花生米小火炒熟盛出',3),
(@r4,3,'生抽、醋、白糖、淀粉调成料汁',2),
(@r4,4,'热锅倒油，滑炒鸡丁至变色盛出',2),
(@r4,5,'锅中加油，爆香干辣椒和花椒，加入葱段翻炒',1),
(@r4,6,'倒入鸡丁和料汁大火翻炒均匀，加入花生米翻匀出锅',2);

-- 5. 清蒸鲈鱼
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('清蒸鲈鱼','鲜嫩味美，原汁原味，粤菜经典','粤菜','简单','蒸',20,320,0,0,1,'system');
SET @r5 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r5,(SELECT id FROM ingredient WHERE name='鲈鱼'),'鲈鱼','1条（约500克）',1),
(@r5,(SELECT id FROM ingredient WHERE name='姜'),'姜丝','适量',2),
(@r5,(SELECT id FROM ingredient WHERE name='葱'),'葱丝','适量',3),
(@r5,(SELECT id FROM ingredient WHERE name='生抽'),'蒸鱼豉油','3勺',4),
(@r5,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r5,1,'鲈鱼处理干净，鱼身两面划几刀，抹料酒和姜片腌制10分钟',12),
(@r5,2,'盘底铺葱姜，放上鱼，水开后大火蒸8分钟',8),
(@r5,3,'倒掉盘中的蒸汁，铺上葱姜丝，淋上蒸鱼豉油',2),
(@r5,4,'热油烧至冒烟，浇在葱姜丝上激出香味',1);

-- 6. 酸辣土豆丝
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('酸辣土豆丝','酸辣爽脆，快手下饭素菜','家常菜','简单','炒',12,185,0,0,1,'system');
SET @r6 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r6,(SELECT id FROM ingredient WHERE name='土豆'),'土豆','2个',1),
(@r6,(SELECT id FROM ingredient WHERE name='干辣椒'),'干辣椒','5个',2),
(@r6,(SELECT id FROM ingredient WHERE name='醋'),'醋','2勺',3),
(@r6,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',4),
(@r6,(SELECT id FROM ingredient WHERE name='葱'),'葱花','少许',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r6,1,'土豆切细丝，放入冷水中浸泡去除淀粉，换水两次',5),
(@r6,2,'土豆丝沥干水分',1),
(@r6,3,'热锅倒油，爆香干辣椒和葱花',1),
(@r6,4,'放入土豆丝大火快炒，加醋和盐调味',3),
(@r6,5,'翻炒均匀即可出锅',2);

-- 7. 糖醋排骨
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('糖醋排骨','酸甜可口，外酥里嫩，宴客必备','家常菜','普通','炖',50,450,0,0,1,'system');
SET @r7 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r7,(SELECT id FROM ingredient WHERE name='排骨'),'排骨','500克',1),
(@r7,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',2),
(@r7,(SELECT id FROM ingredient WHERE name='老抽'),'老抽','半勺',3),
(@r7,(SELECT id FROM ingredient WHERE name='醋'),'醋','3勺',4),
(@r7,(SELECT id FROM ingredient WHERE name='白糖'),'白糖','3勺',5),
(@r7,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',6),
(@r7,(SELECT id FROM ingredient WHERE name='葱'),'葱段','适量',7);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r7,1,'排骨剁小段，冷水下锅焯水去血沫，捞出洗净',8),
(@r7,2,'锅中放油，小火炒化白糖至焦糖色',3),
(@r7,3,'放入排骨翻炒上色',2),
(@r7,4,'加入料酒、生抽、老抽、醋和没过排骨的热水',2),
(@r7,5,'大火烧开转小火炖30分钟',30),
(@r7,6,'大火收汁至汤汁浓稠裹住排骨，撒芝麻点缀',5);

-- 8. 鱼香肉丝
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('鱼香肉丝','虽然没有鱼，但酸甜微辣的鱼香味让人欲罢不能','川菜','普通','炒',20,350,0,0,1,'system');
SET @r8 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r8,(SELECT id FROM ingredient WHERE name='猪里脊'),'猪里脊','300克',1),
(@r8,(SELECT id FROM ingredient WHERE name='木耳'),'木耳','50克',2),
(@r8,(SELECT id FROM ingredient WHERE name='青椒'),'青椒','1个',3),
(@r8,(SELECT id FROM ingredient WHERE name='胡萝卜'),'胡萝卜','半根',4),
(@r8,(SELECT id FROM ingredient WHERE name='豆瓣酱'),'豆瓣酱','1大勺',5),
(@r8,(SELECT id FROM ingredient WHERE name='醋'),'醋','2勺',6),
(@r8,(SELECT id FROM ingredient WHERE name='白糖'),'白糖','2勺',7);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r8,1,'猪里脊切丝，加料酒、淀粉抓匀腌制10分钟',10),
(@r8,2,'木耳泡发切丝，青椒、胡萝卜切丝',5),
(@r8,3,'醋、白糖、生抽、淀粉调成鱼香汁',2),
(@r8,4,'热锅倒油，滑炒肉丝至变色盛出',2),
(@r8,5,'锅中加油，炒香豆瓣酱，放胡萝卜丝、木耳丝炒软',2),
(@r8,6,'倒回肉丝和青椒丝，淋入鱼香汁大火翻炒均匀',2);

-- 9. 地三鲜
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('地三鲜','东北经典素菜，茄子土豆青椒的完美组合','家常菜','普通','炒',25,280,0,0,1,'system');
SET @r9 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r9,(SELECT id FROM ingredient WHERE name='土豆'),'土豆','1个',1),
(@r9,(SELECT id FROM ingredient WHERE name='茄子'),'茄子','1根',2),
(@r9,(SELECT id FROM ingredient WHERE name='青椒'),'青椒','2个',3),
(@r9,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',4),
(@r9,(SELECT id FROM ingredient WHERE name='蒜'),'蒜末','适量',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r9,1,'土豆去皮切片，茄子切块，青椒切块',5),
(@r9,2,'锅中多油，分别炸土豆片至金黄，茄子炸软，捞出控油',8),
(@r9,3,'锅中留底油，爆香蒜末',1),
(@r9,4,'放入青椒翻炒，倒入土豆和茄子',2),
(@r9,5,'加生抽、盐调味，快速翻炒均匀出锅',2);

-- 10. 手撕包菜
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('手撕包菜','酸辣脆爽，简单下饭的平民美味','家常菜','简单','炒',10,145,0,0,1,'system');
SET @r10 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r10,(SELECT id FROM ingredient WHERE name='包菜'),'包菜','半棵',1),
(@r10,(SELECT id FROM ingredient WHERE name='干辣椒'),'干辣椒','5个',2),
(@r10,(SELECT id FROM ingredient WHERE name='醋'),'醋','1勺',3),
(@r10,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',4),
(@r10,(SELECT id FROM ingredient WHERE name='蒜'),'蒜片','3瓣',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r10,1,'包菜用手撕成小片，洗净沥干水分',3),
(@r10,2,'热锅倒油，爆香干辣椒和蒜片',1),
(@r10,3,'放入包菜大火快速翻炒至变软',2),
(@r10,4,'沿锅边淋入醋，加盐调味，翻炒均匀出锅',2);

-- 11. 可乐鸡翅
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('可乐鸡翅','甜嫩可口，零失败新手菜','家常菜','简单','炖',30,380,0,0,1,'system');
SET @r11 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r11,(SELECT id FROM ingredient WHERE name='鸡翅'),'鸡翅中','10个',1),
(@r11,(SELECT id FROM ingredient WHERE name='可乐'),'可乐','1瓶',2),
(@r11,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',3),
(@r11,(SELECT id FROM ingredient WHERE name='姜'),'姜片','3片',4),
(@r11,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r11,1,'鸡翅洗净，两面划刀，冷水下锅焯水捞出',5),
(@r11,2,'热锅倒油，煎鸡翅两面至金黄',5),
(@r11,3,'加入姜片、料酒、生抽翻炒',1),
(@r11,4,'倒入可乐没过鸡翅，大火烧开转小火炖20分钟',20),
(@r11,5,'转大火收汁至浓稠，装盘撒芝麻',3);

-- 12. 回锅肉
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('回锅肉','川菜中的扛把子，肥而不腻，下饭神器','川菜','普通','炒',30,480,0,0,1,'system');
SET @r12 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r12,(SELECT id FROM ingredient WHERE name='猪五花肉'),'五花肉','400克',1),
(@r12,(SELECT id FROM ingredient WHERE name='蒜苗'),'蒜苗','3根',2),
(@r12,(SELECT id FROM ingredient WHERE name='豆瓣酱'),'豆瓣酱','1大勺',3),
(@r12,(SELECT id FROM ingredient WHERE name='青椒'),'青椒','2个',4),
(@r12,(SELECT id FROM ingredient WHERE name='姜'),'姜片','3片',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r12,1,'五花肉冷水下锅加姜片煮20分钟至筷子能插透',20),
(@r12,2,'捞出放凉切薄片',3),
(@r12,3,'蒜苗切斜段，青椒切块',2),
(@r12,4,'锅中不放油，直接煸炒肉片至出油卷起呈灯盏状',3),
(@r12,5,'将肉推到一边，中间炒香豆瓣酱',1),
(@r12,6,'混合翻炒后加入蒜苗和青椒炒至断生即可',2);

-- 13. 蒜蓉西兰花
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('蒜蓉西兰花','清爽脆嫩，健康低卡','家常菜','简单','焯+炒',10,120,0,0,1,'system');
SET @r13 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r13,(SELECT id FROM ingredient WHERE name='西兰花'),'西兰花','1棵',1),
(@r13,(SELECT id FROM ingredient WHERE name='蒜'),'蒜末','3瓣',2),
(@r13,(SELECT id FROM ingredient WHERE name='蚝油'),'蚝油','1勺',3),
(@r13,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',4);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r13,1,'西兰花掰成小朵，盐水浸泡10分钟后洗净',10),
(@r13,2,'烧开水，加少许盐和油，焯烫西兰花1分钟捞出',2),
(@r13,3,'热锅倒油，小火炒香蒜末',1),
(@r13,4,'放入西兰花，加蚝油和盐快速翻炒均匀即可',2);

-- 14. 水煮肉片
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('水煮肉片','麻辣鲜香，肉片嫩滑，川菜经典大菜','川菜','困难','煮',35,520,0,0,1,'system');
SET @r14 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r14,(SELECT id FROM ingredient WHERE name='猪里脊'),'猪里脊','300克',1),
(@r14,(SELECT id FROM ingredient WHERE name='豆芽'),'豆芽','150克',2),
(@r14,(SELECT id FROM ingredient WHERE name='生菜'),'生菜','100克',3),
(@r14,(SELECT id FROM ingredient WHERE name='豆瓣酱'),'豆瓣酱','2大勺',4),
(@r14,(SELECT id FROM ingredient WHERE name='干辣椒'),'干辣椒','10个',5),
(@r14,(SELECT id FROM ingredient WHERE name='花椒'),'花椒','1大勺',6),
(@r14,(SELECT id FROM ingredient WHERE name='淀粉'),'淀粉','适量',7);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r14,1,'猪里脊切薄片，加料酒、盐、淀粉抓匀腌制15分钟',15),
(@r14,2,'豆芽、生菜焯水后铺在碗底',2),
(@r14,3,'锅中倒油，炒香豆瓣酱出红油，加清水烧开',5),
(@r14,4,'逐片下入肉片，煮至变色浮起，连汤倒入碗中',3),
(@r14,5,'撒上干辣椒段和花椒，浇热油激出香味',2);

-- 15. 家常豆腐
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('家常豆腐','外焦里嫩，咸香入味','家常菜','简单','煎',15,230,0,0,1,'system');
SET @r15 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r15,(SELECT id FROM ingredient WHERE name='豆腐'),'老豆腐','400克',1),
(@r15,(SELECT id FROM ingredient WHERE name='青椒'),'青椒','1个',2),
(@r15,(SELECT id FROM ingredient WHERE name='木耳'),'木耳','30克',3),
(@r15,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',4),
(@r15,(SELECT id FROM ingredient WHERE name='葱'),'葱花','适量',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r15,1,'豆腐切厚片，青椒切块，木耳泡发撕小朵',5),
(@r15,2,'平底锅倒油，将豆腐煎至两面金黄盛出',5),
(@r15,3,'锅中爆香葱花，放入青椒和木耳翻炒',2),
(@r15,4,'倒回豆腐，加生抽、盐和少许水，焖2分钟入味',3),
(@r15,5,'大火收汁即可',1);

-- 16. 蛋炒饭
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('蛋炒饭','粒粒分明，简单美味，最暖心的家常主食','主食','简单','炒',10,380,0,0,1,'system');
SET @r16 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r16,(SELECT id FROM ingredient WHERE name='大米'),'隔夜米饭','2碗',1),
(@r16,(SELECT id FROM ingredient WHERE name='鸡蛋'),'鸡蛋','2个',2),
(@r16,(SELECT id FROM ingredient WHERE name='葱'),'葱花','适量',3),
(@r16,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',4);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r16,1,'鸡蛋打散，隔夜米饭提前揉散',2),
(@r16,2,'热锅倒油，倒入蛋液快速搅散至半凝固',1),
(@r16,3,'倒入米饭大火快速翻炒，使每粒米都裹上蛋',3),
(@r16,4,'加盐调味，撒葱花翻炒均匀出锅',1);

-- 17. 白切鸡
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('白切鸡','皮爽肉滑，原汁原味，粤菜代表作','粤菜','普通','煮',45,420,0,0,1,'system');
SET @r17 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r17,(SELECT id FROM ingredient WHERE name='鸡腿'),'三黄鸡','1只（约1.5kg）',1),
(@r17,(SELECT id FROM ingredient WHERE name='姜'),'姜片','5片',2),
(@r17,(SELECT id FROM ingredient WHERE name='葱'),'葱段','3段',3),
(@r17,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','2勺',4);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r17,1,'鸡处理干净，沥干水分',3),
(@r17,2,'锅中加足够水，放入姜片、葱段、料酒，大火烧开',5),
(@r17,3,'手提鸡颈，将鸡放入开水中烫10秒提起，重复3次',1),
(@r17,4,'将鸡完全浸入锅中，小火保持微沸煮20分钟',20),
(@r17,5,'关火浸泡10分钟，取出放入冰水中冷却',12),
(@r17,6,'沥干斩块，佐以姜葱蘸料食用',5);

-- 18. 冬瓜排骨汤
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('冬瓜排骨汤','清淡鲜美，消暑解腻的家常汤品','家常菜','简单','炖',60,280,0,0,1,'system');
SET @r18 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r18,(SELECT id FROM ingredient WHERE name='排骨'),'排骨','400克',1),
(@r18,(SELECT id FROM ingredient WHERE name='冬瓜'),'冬瓜','500克',2),
(@r18,(SELECT id FROM ingredient WHERE name='姜'),'姜片','3片',3),
(@r18,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',4);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r18,1,'排骨焯水去血沫，捞出洗净',5),
(@r18,2,'冬瓜去皮去瓤切厚块',3),
(@r18,3,'排骨加姜片和足量水，大火烧开转小火炖40分钟',40),
(@r18,4,'加入冬瓜继续炖15分钟至冬瓜透明',15),
(@r18,5,'加盐调味即可',1);

-- 19. 酸辣汤
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('酸辣汤','酸辣开胃，暖身驱寒，经典中式汤品','家常菜','简单','煮',15,150,0,0,1,'system');
SET @r19 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r19,(SELECT id FROM ingredient WHERE name='豆腐'),'嫩豆腐','200克',1),
(@r19,(SELECT id FROM ingredient WHERE name='鸡蛋'),'鸡蛋','1个',2),
(@r19,(SELECT id FROM ingredient WHERE name='香菇'),'香菇','3朵',3),
(@r19,(SELECT id FROM ingredient WHERE name='醋'),'醋','2勺',4),
(@r19,(SELECT id FROM ingredient WHERE name='胡椒粉'),'白胡椒粉','1小勺',5),
(@r19,(SELECT id FROM ingredient WHERE name='淀粉'),'水淀粉','适量',6);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r19,1,'豆腐切丝，香菇切丝，鸡蛋打散',3),
(@r19,2,'锅中加清水烧开，放入豆腐丝、香菇丝煮3分钟',3),
(@r19,3,'加入醋、盐、胡椒粉调味',1),
(@r19,4,'淋入水淀粉勾芡至浓稠',1),
(@r19,5,'转圈淋入蛋液成蛋花，出锅撒葱花',1);

-- 20. 干炒牛河
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('干炒牛河','锅气十足，河粉爽滑，经典粤式炒粉','粤菜','普通','炒',20,420,0,0,1,'system');
SET @r20 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r20,(SELECT id FROM ingredient WHERE name='河粉'),'河粉','400克',1),
(@r20,(SELECT id FROM ingredient WHERE name='牛肉'),'牛肉','150克',2),
(@r20,(SELECT id FROM ingredient WHERE name='豆芽'),'豆芽','100克',3),
(@r20,(SELECT id FROM ingredient WHERE name='葱'),'葱段','适量',4),
(@r20,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',5),
(@r20,(SELECT id FROM ingredient WHERE name='淀粉'),'淀粉','适量',6);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r20,1,'牛肉切薄片，加生抽、淀粉抓匀腌制10分钟',10),
(@r20,2,'河粉用手撕散，防止粘连',1),
(@r20,3,'热锅倒油，滑炒牛肉至变色盛出',2),
(@r20,4,'锅中加油，放豆芽和河粉大火快炒',2),
(@r20,5,'加入牛肉和葱段，淋入生抽，翻炒均匀出锅',2);

-- 21. 蒜蓉粉丝虾
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('蒜蓉粉丝虾','蒜香浓郁，粉丝吸满虾汁，宴客硬菜','家常菜','普通','蒸',15,280,0,0,1,'system');
SET @r21 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r21,(SELECT id FROM ingredient WHERE name='虾'),'大虾','12只',1),
(@r21,(SELECT id FROM ingredient WHERE name='粉丝'),'粉丝','1把',2),
(@r21,(SELECT id FROM ingredient WHERE name='蒜蓉'),'蒜蓉','3大勺',3),
(@r21,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',4),
(@r21,(SELECT id FROM ingredient WHERE name='葱'),'葱花','适量',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r21,1,'粉丝温水泡软，大虾开背去虾线',5),
(@r21,2,'粉丝铺在盘底，摆上大虾',2),
(@r21,3,'锅中倒油，小火炒香蒜蓉，加生抽调成蒜蓉酱',2),
(@r21,4,'将蒜蓉酱均匀浇在大虾上，水开后蒸8分钟',8),
(@r21,5,'出锅撒葱花，淋热油激香',1);

-- 22. 辣子鸡
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('辣子鸡','麻辣干香，在辣椒堆里找鸡丁的乐趣','川菜','普通','炸+炒',35,460,0,0,1,'system');
SET @r22 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r22,(SELECT id FROM ingredient WHERE name='鸡腿'),'鸡腿','500克',1),
(@r22,(SELECT id FROM ingredient WHERE name='干辣椒'),'干辣椒','50克',2),
(@r22,(SELECT id FROM ingredient WHERE name='花椒'),'花椒','1大勺',3),
(@r22,(SELECT id FROM ingredient WHERE name='姜'),'姜片','3片',4),
(@r22,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',5),
(@r22,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',6);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r22,1,'鸡腿去骨切小丁，加料酒、生抽、姜片腌制20分钟',20),
(@r22,2,'腌好的鸡丁裹上薄薄一层淀粉',2),
(@r22,3,'油温六成热，炸鸡丁至金黄酥脆捞出',5),
(@r22,4,'锅中留底油，小火炒香干辣椒和花椒',2),
(@r22,5,'倒入鸡丁大火翻炒，撒盐和白糖调味',2);

-- 23. 葱油拌面
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('葱油拌面','葱香四溢，简单快手，上海经典','家常菜','简单','煮+拌',15,350,0,0,1,'system');
SET @r23 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r23,(SELECT id FROM ingredient WHERE name='面条'),'挂面','200克',1),
(@r23,(SELECT id FROM ingredient WHERE name='葱'),'小葱','1把',2),
(@r23,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','3勺',3),
(@r23,(SELECT id FROM ingredient WHERE name='老抽'),'老抽','1勺',4),
(@r23,(SELECT id FROM ingredient WHERE name='白糖'),'白糖','1小勺',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r23,1,'小葱切段，只用葱绿部分',2),
(@r23,2,'锅中多倒油，小火炸葱段至焦黄捞出（葱油即成）',8),
(@r23,3,'生抽、老抽、白糖调成酱汁',1),
(@r23,4,'煮面至八分熟捞出，过凉水沥干',3),
(@r23,5,'淋上葱油和酱汁拌匀即可',1);

-- 24. 红烧带鱼
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('红烧带鱼','咸鲜入味，肉质细嫩，下饭好菜','家常菜','普通','炖',25,320,0,0,1,'system');
SET @r24 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r24,(SELECT id FROM ingredient WHERE name='带鱼'),'带鱼','400克',1),
(@r24,(SELECT id FROM ingredient WHERE name='葱'),'葱段','适量',2),
(@r24,(SELECT id FROM ingredient WHERE name='姜'),'姜片','3片',3),
(@r24,(SELECT id FROM ingredient WHERE name='蒜'),'蒜瓣','3瓣',4),
(@r24,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',5),
(@r24,(SELECT id FROM ingredient WHERE name='老抽'),'老抽','半勺',6);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r24,1,'带鱼处理干净切段，加料酒和姜片腌制10分钟',10),
(@r24,2,'擦干水分，两面拍薄薄一层面粉',1),
(@r24,3,'平底锅倒油，煎带鱼至两面金黄',5),
(@r24,4,'加入葱姜蒜爆香，烹入生抽、老抽、醋和热水',2),
(@r24,5,'小火炖8分钟，大火收汁即可',10);

-- 25. 青椒肉丝
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('青椒肉丝','简单快手，家常便饭中的经典','家常菜','简单','炒',15,300,0,0,1,'system');
SET @r25 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r25,(SELECT id FROM ingredient WHERE name='猪瘦肉'),'猪瘦肉','250克',1),
(@r25,(SELECT id FROM ingredient WHERE name='青椒'),'青椒','3个',2),
(@r25,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',3),
(@r25,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',4),
(@r25,(SELECT id FROM ingredient WHERE name='淀粉'),'淀粉','适量',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r25,1,'猪瘦肉切丝，加料酒、生抽、淀粉抓匀腌制5分钟',5),
(@r25,2,'青椒去籽切丝',2),
(@r25,3,'热锅倒油，滑炒肉丝至变色盛出',2),
(@r25,4,'锅中留底油，炒青椒丝至断生',2),
(@r25,5,'倒回肉丝，加盐和生抽调味，快速翻炒均匀',2);

-- 26. 紫菜蛋花汤
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('紫菜蛋花汤','清淡鲜美，三分钟快汤','家常菜','简单','煮',5,80,0,0,1,'system');
SET @r26 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r26,(SELECT id FROM ingredient WHERE name='紫菜'),'紫菜','1片',1),
(@r26,(SELECT id FROM ingredient WHERE name='鸡蛋'),'鸡蛋','1个',2),
(@r26,(SELECT id FROM ingredient WHERE name='虾皮'),'虾皮','少许',3),
(@r26,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',4),
(@r26,(SELECT id FROM ingredient WHERE name='葱'),'葱花','少许',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r26,1,'鸡蛋打散，紫菜撕碎放入碗中',1),
(@r26,2,'锅中烧水，水开后放入虾皮',1),
(@r26,3,'转圈淋入蛋液，用筷子搅散成蛋花',1),
(@r26,4,'加盐调味，倒入放有紫菜的碗中，撒葱花',1);

-- 27. 番茄蛋汤
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('番茄蛋汤','酸甜开胃，家常最温暖的汤','家常菜','简单','煮',10,120,0,0,1,'system');
SET @r27 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r27,(SELECT id FROM ingredient WHERE name='番茄'),'番茄','2个',1),
(@r27,(SELECT id FROM ingredient WHERE name='鸡蛋'),'鸡蛋','2个',2),
(@r27,(SELECT id FROM ingredient WHERE name='盐'),'盐','适量',3),
(@r27,(SELECT id FROM ingredient WHERE name='葱'),'葱花','少许',4);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r27,1,'番茄去皮切块，鸡蛋打散',2),
(@r27,2,'热锅倒油，炒番茄至出汁',3),
(@r27,3,'加适量清水烧开',1),
(@r27,4,'淋入蛋液搅散成蛋花，加盐调味',1),
(@r27,5,'撒葱花出锅',1);

-- 28. 红烧茄子
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('红烧茄子','软糯入味，比肉还好吃的素菜','家常菜','简单','烧',20,260,0,0,1,'system');
SET @r28 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r28,(SELECT id FROM ingredient WHERE name='茄子'),'紫茄子','2根',1),
(@r28,(SELECT id FROM ingredient WHERE name='蒜'),'蒜末','3瓣',2),
(@r28,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',3),
(@r28,(SELECT id FROM ingredient WHERE name='白糖'),'白糖','1小勺',4),
(@r28,(SELECT id FROM ingredient WHERE name='葱'),'葱花','适量',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r28,1,'茄子滚刀切块，撒盐腌制10分钟挤干水分',12),
(@r28,2,'热锅多油，煎茄子至表面金黄变软',5),
(@r28,3,'加蒜末爆香',1),
(@r28,4,'加生抽、白糖和少许水，焖2分钟入味',3),
(@r28,5,'大火收汁，撒葱花出锅',1);

-- 29. 剁椒鱼头
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('剁椒鱼头','鲜辣嫩滑，湘菜经典，宴客超有面','川菜','普通','蒸',25,350,0,0,1,'system');
SET @r29 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r29,(SELECT id FROM ingredient WHERE name='鲢鱼头'),'胖头鱼头','1个（约800克）',1),
(@r29,(SELECT id FROM ingredient WHERE name='姜'),'姜片','5片',2),
(@r29,(SELECT id FROM ingredient WHERE name='葱'),'葱段','适量',3),
(@r29,(SELECT id FROM ingredient WHERE name='辣椒酱'),'剁椒酱','3大勺',4),
(@r29,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r29,1,'鱼头处理干净，从中间劈开不切断',3),
(@r29,2,'鱼头抹料酒、姜片腌制10分钟',10),
(@r29,3,'盘底铺葱段和姜片，放上鱼头',1),
(@r29,4,'均匀铺上剁椒酱，水开后蒸12分钟',12),
(@r29,5,'取出撒葱花，淋上热油即可',2);

-- 30. 煲仔饭
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('煲仔饭','焦香锅巴，腊味飘香，港式经典','粤菜','困难','焖',40,450,0,0,1,'system');
SET @r30 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r30,(SELECT id FROM ingredient WHERE name='大米'),'大米','200克',1),
(@r30,(SELECT id FROM ingredient WHERE name='腊肉'),'腊肠','2根',2),
(@r30,(SELECT id FROM ingredient WHERE name='青菜'),'油菜','2棵',3),
(@r30,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','2勺',4),
(@r30,(SELECT id FROM ingredient WHERE name='蚝油'),'蚝油','1勺',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r30,1,'大米提前浸泡30分钟',30),
(@r30,2,'砂锅底刷油，放入泡好的米和适量水',2),
(@r30,3,'大火烧开转小火焖至水分收干',10),
(@r30,4,'铺上切好的腊肠片，盖盖沿锅边淋一圈油，小火焖10分钟',12),
(@r30,5,'同时烫熟油菜，生抽加蚝油调成酱汁',3),
(@r30,6,'关火后焖3分钟，放入油菜，淋酱汁拌匀',3);

-- 31. 叉烧
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('叉烧','色泽红亮，甜香可口，广式经典烧味','粤菜','普通','烤',60,480,0,0,1,'system');
SET @r31 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r31,(SELECT id FROM ingredient WHERE name='猪五花肉'),'猪梅花肉','500克',1),
(@r31,(SELECT id FROM ingredient WHERE name='生抽'),'生抽','3勺',2),
(@r31,(SELECT id FROM ingredient WHERE name='蚝油'),'蚝油','2勺',3),
(@r31,(SELECT id FROM ingredient WHERE name='料酒'),'料酒','1勺',4),
(@r31,(SELECT id FROM ingredient WHERE name='白糖'),'蜂蜜','2勺',5);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r31,1,'梅花肉切长条，用叉子扎孔方便入味',2),
(@r31,2,'生抽、蚝油、料酒、白糖、蒜末调成叉烧酱，腌制4小时',240),
(@r31,3,'烤箱预热200度，肉条放烤架上，刷一层叉烧酱',5),
(@r31,4,'中层烤20分钟，翻面刷酱再烤15分钟',35),
(@r31,5,'取出刷蜂蜜，再烤5分钟至表面焦香',7),
(@r31,6,'放凉后切片装盘',5);

-- 32. 夫妻肺片
INSERT INTO recipe (name, description, cuisine, difficulty, cook_method, cook_time, calories, view_count, favorite_count, status, source) VALUES
('夫妻肺片','麻辣鲜香，红油透亮，川味凉菜之王','川菜','普通','拌',40,280,0,0,1,'system');
SET @r32 = LAST_INSERT_ID();
INSERT INTO recipe_ingredient (recipe_id, ingredient_id, name, amount, sort_order) VALUES
(@r32,(SELECT id FROM ingredient WHERE name='牛肉'),'牛腱子','200克',1),
(@r32,(SELECT id FROM ingredient WHERE name='牛肚'),'牛杂','200克',2),
(@r32,(SELECT id FROM ingredient WHERE name='花生'),'花生碎','30克',3),
(@r32,(SELECT id FROM ingredient WHERE name='芝麻'),'白芝麻','1勺',4),
(@r32,(SELECT id FROM ingredient WHERE name='辣椒油'),'辣椒油','3勺',5),
(@r32,(SELECT id FROM ingredient WHERE name='花椒'),'花椒粉','1小勺',6);
INSERT INTO recipe_step (recipe_id, step_no, content, duration) VALUES
(@r32,1,'牛腱子和牛杂冷水下锅，加姜片、料酒焯水去腥',10),
(@r32,2,'换清水加八角、桂皮、姜片，煮至熟透（约40分钟）',40),
(@r32,3,'捞出放凉切薄片',3),
(@r32,4,'辣椒油、生抽、醋、白糖、花椒粉调成红油汁',2),
(@r32,5,'将料汁浇在肉片上，撒花生碎和芝麻，拌匀即可',2);
