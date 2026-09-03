package com.booking.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.entity.Admin;
import com.booking.entity.House;
import com.booking.entity.Room;
import com.booking.mapper.AdminMapper;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据库初始化：建表 + 旧库字段迁移 + 首次启动写入种子数据（房源/房型/商家账号）
 */
@Component
public class DbInit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DbInit.class);

    private final JdbcTemplate jdbc;
    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;
    private final AdminMapper adminMapper;

    public DbInit(JdbcTemplate jdbc, HouseMapper houseMapper, RoomMapper roomMapper, AdminMapper adminMapper) {
        this.jdbc = jdbc;
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
        this.adminMapper = adminMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        createTables();
        migrateExisting();
        seedIfEmpty();
        seedAdmin();
        log.info("数据库初始化完成");
    }

    private void createTables() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS house (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                area TEXT NOT NULL,
                address TEXT,
                score REAL DEFAULT 0,
                reviews INTEGER DEFAULT 0,
                price REAL DEFAULT 0,
                description TEXT,
                tags TEXT,
                img_prompt TEXT,
                status INTEGER DEFAULT 1
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS room (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                house_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                price REAL DEFAULT 0,
                stock INTEGER DEFAULT 0,
                img TEXT
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS booking_order (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_no TEXT UNIQUE NOT NULL,
                house_id INTEGER NOT NULL,
                room_id INTEGER NOT NULL,
                guest_name TEXT NOT NULL,
                phone TEXT NOT NULL,
                check_in TEXT,
                check_out TEXT,
                nights INTEGER DEFAULT 1,
                amount REAL DEFAULT 0,
                status TEXT DEFAULT '待确认',
                remark TEXT,
                create_time TEXT DEFAULT (datetime('now', 'localtime')),
                confirm_time TEXT,
                check_in_time TEXT,
                complete_time TEXT,
                cancel_reason TEXT
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS admin (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                nickname TEXT,
                role TEXT DEFAULT 'merchant',
                merchant_id INTEGER DEFAULT 0,
                merchant_name TEXT,
                contact_phone TEXT,
                apply_status TEXT DEFAULT '通过',
                reject_reason TEXT,
                description TEXT,
                open_status INTEGER DEFAULT 1,
                status INTEGER DEFAULT 1,
                create_time TEXT DEFAULT (datetime('now', 'localtime'))
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS review (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                house_id INTEGER NOT NULL,
                guest_name TEXT NOT NULL,
                phone TEXT,
                rating INTEGER DEFAULT 5,
                content TEXT,
                reply TEXT,
                status INTEGER DEFAULT 1,
                create_time TEXT DEFAULT (datetime('now', 'localtime')),
                reply_time TEXT
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS banner (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                house_id INTEGER NOT NULL,
                image_url TEXT NOT NULL,
                title TEXT,
                sort INTEGER DEFAULT 0,
                status INTEGER DEFAULT 1,
                create_time TEXT DEFAULT (datetime('now', 'localtime'))
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS wx_user (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                openid TEXT UNIQUE NOT NULL,
                phone TEXT,
                create_time TEXT DEFAULT (datetime('now', 'localtime'))
            )
            """);
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS favorite (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                house_id INTEGER NOT NULL,
                openid TEXT NOT NULL,
                create_time TEXT DEFAULT (datetime('now', 'localtime')),
                UNIQUE (house_id, openid)
            )
            """);
        // 登录令牌持久化：后端重启后仍保持登录态
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS admin_token (
                token TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                expire_time TEXT NOT NULL
            )
            """);
        // 兼容旧库：为 booking_order 补 openid 列（已存在则忽略）
        try {
            jdbc.execute("ALTER TABLE booking_order ADD COLUMN openid TEXT");
        } catch (Exception ignored) {
        }
    }

    /** 旧库字段迁移：SQLite 无 ADD COLUMN IF NOT EXISTS，需先查 PRAGMA */
    private void migrateExisting() {
        ensureColumn("house", "status", "ALTER TABLE house ADD COLUMN status INTEGER DEFAULT 1");
        ensureColumn("house", "merchant_id", "ALTER TABLE house ADD COLUMN merchant_id INTEGER DEFAULT 0");
        ensureColumn("room", "img", "ALTER TABLE room ADD COLUMN img TEXT");
        ensureColumn("booking_order", "confirm_time", "ALTER TABLE booking_order ADD COLUMN confirm_time TEXT");
        ensureColumn("booking_order", "cancel_reason", "ALTER TABLE booking_order ADD COLUMN cancel_reason TEXT");
        ensureColumn("booking_order", "check_in_time", "ALTER TABLE booking_order ADD COLUMN check_in_time TEXT");
        ensureColumn("booking_order", "complete_time", "ALTER TABLE booking_order ADD COLUMN complete_time TEXT");
        ensureColumn("admin", "role", "ALTER TABLE admin ADD COLUMN role TEXT DEFAULT 'merchant'");
        ensureColumn("admin", "merchant_id", "ALTER TABLE admin ADD COLUMN merchant_id INTEGER DEFAULT 0");
        ensureColumn("admin", "merchant_name", "ALTER TABLE admin ADD COLUMN merchant_name TEXT");
        ensureColumn("admin", "contact_phone", "ALTER TABLE admin ADD COLUMN contact_phone TEXT");
        ensureColumn("admin", "apply_status", "ALTER TABLE admin ADD COLUMN apply_status TEXT DEFAULT '通过'");
        ensureColumn("admin", "reject_reason", "ALTER TABLE admin ADD COLUMN reject_reason TEXT");
        ensureColumn("admin", "description", "ALTER TABLE admin ADD COLUMN description TEXT");
        ensureColumn("admin", "open_status", "ALTER TABLE admin ADD COLUMN open_status INTEGER DEFAULT 1");
        ensureColumn("admin", "status", "ALTER TABLE admin ADD COLUMN status INTEGER DEFAULT 1");
        ensureColumn("admin", "address", "ALTER TABLE admin ADD COLUMN address TEXT");
        ensureColumn("admin", "lng", "ALTER TABLE admin ADD COLUMN lng REAL");
        ensureColumn("admin", "lat", "ALTER TABLE admin ADD COLUMN lat REAL");

        // 存量数据归属：已有房源归商家1（林海小居）
        try {
            jdbc.update("UPDATE house SET merchant_id = 1 WHERE merchant_id IS NULL OR merchant_id = 0");
        } catch (Exception ignored) {
        }
    }

    private void ensureColumn(String table, String column, String alterSql) {
        try {
            // PRAGMA table_info 返回多列(cid,name,type,...)，需按 name 判断列是否存在
            List<java.util.Map<String, Object>> cols = jdbc.queryForList("PRAGMA table_info(" + table + ")");
            boolean exists = cols.stream().anyMatch(m -> column.equalsIgnoreCase(String.valueOf(m.get("name"))));
            if (!exists) {
                jdbc.execute(alterSql);
                log.info("迁移: {}.{} 已补充", table, column);
            }
        } catch (Exception e) {
            log.warn("迁移 {}.{} 跳过: {}", table, column, e.getMessage());
        }
    }

    private void seedIfEmpty() {
        Long count = houseMapper.selectCount(new QueryWrapper<>());
        if (count != null && count > 0) return;

        log.info("首次启动，写入房源种子数据...");

        insertHouse("林海小居·湖景庭院民宿", "民宿", "临湖度假区", "湖滨南路 88 号", 4.9, 128, 268,
                "推窗见湖，白墙黛瓦的江南庭院民宿，步行 3 分钟即达湖畔栈道，适合周末度假与家庭出行。",
                "湖景,庭院,含早餐",
                "a cozy lakeside homestay courtyard in southern China, white walls dark tiles traditional architecture, calm lake view, warm evening light, photorealistic",
                List.of(
                        room("湖景大床房", 268, 3),
                        room("庭院双床房", 238, 5),
                        room("湖景家庭套房", 458, 2)
                ));
        insertHouse("云溪山居·星空木屋", "民宿", "云溪山景区", "云溪山半山腰", 4.8, 96, 388,
                "山谷间的全木结构小屋，天花板设观星天窗，夜晚躺在床上数星星，清晨被鸟鸣唤醒。",
                "山景,星空顶,篝火",
                "a wooden cabin with glass skylight roof for stargazing in mountain forest, cozy warm interior glow at night, surrounded by pine trees, photorealistic",
                List.of(
                        room("星空大床房", 388, 2),
                        room("观景双人房", 328, 3)
                ));
        insertHouse("半山云宿·江景大床房", "民宿", "滨江路", "滨江路 12 号", 4.7, 210, 328,
                "270° 落地窗直面江面，日落时分江景尽收眼底。一楼自带精品咖啡吧，入住赠手冲咖啡两杯。",
                "江景,落地窗,咖啡",
                "a modern riverside guesthouse room with large floor-to-ceiling window facing scenic river view, minimalist cozy interior, golden sunset light, photorealistic",
                List.of(
                        room("江景大床房", 328, 4),
                        room("江景双床房", 358, 3)
                ));
        insertHouse("溪谷小筑·亲子主题民宿", "民宿", "溪谷温泉小镇", "温泉大道 36 号", 4.8, 173, 298,
                "专为亲子家庭设计的主题民宿，配有室内儿童乐园、绘本角与家庭温泉池，带娃出行首选。",
                "亲子,温泉,儿童乐园",
                "a family-friendly courtyard homestay with colorful kids playground and warm lanterns, cheerful atmosphere, cozy Chinese style courtyard, photorealistic",
                List.of(
                        room("亲子主题房", 298, 6),
                        room("家庭温泉套房", 468, 2)
                ));
        insertHouse("清涧别院·四合院雅居", "民宿", "古城历史文化区", "古城东街 5 号", 4.9, 145, 458,
                "百年四合院改造的雅致民宿，青砖黛瓦、回廊水榭，每日午后有免费茶艺体验。",
                "庭院,文化,茶艺",
                "an elegant traditional Chinese courtyard siheyuan guesthouse with refined garden, ponds and stone paths, classical architecture, soft morning light, photorealistic",
                List.of(
                        room("四合院大床房", 458, 4),
                        room("回廊景观房", 528, 2)
                ));
        insertHouse("澜庭酒店·行政套房", "酒店", "CBD 商务区", "中心大道 100 号", 4.6, 328, 398,
                "城市中心的高端商务酒店，配套健身房、恒温泳池与全日自助餐厅，行政楼层享专属礼遇。",
                "商务,健身房,自助餐",
                "a modern upscale hotel executive suite with elegant interior design, city skyline view from large window, warm lighting, photorealistic",
                List.of(
                        room("行政大床房", 398, 8),
                        room("行政套房", 688, 3)
                ));
        insertHouse("悦享国际酒店·豪华标间", "酒店", "火车站商圈", "站前路 1 号", 4.5, 452, 238,
                "毗邻高铁站的连锁酒店，免费停车与自助早餐，是差旅与中转住宿的高性价比之选。",
                "交通便利,自助早餐,免费停车",
                "a modern hotel deluxe twin room, clean bright interior with two beds, contemporary design, daylight, photorealistic",
                List.of(
                        room("豪华双床房", 238, 12),
                        room("豪华大床房", 258, 10)
                ));
        insertHouse("望山雅居·复式Loft", "民宿", "望山艺术区", "望山路 66 号", 4.7, 88, 418,
                "设计师打造的复式 Loft 民宿，楼下会客、楼上就寝，顶层露台可远眺望山全景。",
                "Loft,设计感,观景露台",
                "a stylish modern loft apartment with mezzanine sleeping area and rooftop terrace view of green hills, designer interior, warm tones, photorealistic",
                List.of(
                        room("复式Loft", 418, 3),
                        room("Loft家庭房", 528, 2)
                ));
    }

    private void seedAdmin() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 商家账号：merchant_admin / 123456（林海小居；用户名 admin 预留给平台开发者）
        Admin merchant = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", "merchant_admin"));
        if (merchant == null) {
            // 兼容旧库：老商家账号曾叫 admin，统一迁移为 merchant_admin
            merchant = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", "admin").eq("role", "merchant"));
            if (merchant != null) {
                merchant.setUsername("merchant_admin");
                adminMapper.updateById(merchant);
                log.info("商家账号已迁移: admin → merchant_admin");
                return;
            }
            merchant = new Admin();
            merchant.setUsername("merchant_admin");
            merchant.setPassword("123456");
            merchant.setNickname("林海小居商家");
            merchant.setRole("merchant");
            merchant.setMerchantId(1);
            merchant.setMerchantName("林海小居");
            merchant.setContactPhone("13800000000");
            merchant.setDescription("临湖而居的江南庭院民宿，湖景、庭院与地道家常菜，适合度假与家庭出行。");
            merchant.setOpenStatus(1);
            merchant.setApplyStatus("通过");
            merchant.setStatus(1);
            merchant.setCreateTime(now);
            adminMapper.insert(merchant);
            log.info("商家账号已初始化: merchant_admin / 123456");
        } else if (merchant.getMerchantId() == null || merchant.getMerchantId() == 0) {
            // 旧库迁移补全：已有商家账号归属到商家1
            merchant.setRole("merchant");
            merchant.setMerchantId(1);
            merchant.setMerchantName("林海小居");
            merchant.setApplyStatus("通过");
            merchant.setStatus(1);
            adminMapper.updateById(merchant);
            log.info("商家账号信息已补全: merchant_admin");
        }

        // 平台开发者账号：admin / 2768（登录需预留手机号 contactPhone 匹配）
        Admin dev = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", "admin").eq("role", "platform"));
        if (dev == null) {
            // 兼容旧库：旧平台账号名为 dev
            dev = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", "dev"));
        }
        if (dev == null) {
            Admin p = new Admin();
            p.setUsername("admin");
            p.setPassword("2768");
            p.setNickname("平台开发者");
            p.setRole("platform");
            p.setMerchantId(0);
            p.setStatus(1);
            // 默认预留手机号：新库首次启动即有绑定，避免"需预留手机号登录"却无法配置的死锁；
            // 登录成功后可在账号设置中修改 contactPhone
            p.setContactPhone("15587571721");
            p.setCreateTime(now);
            adminMapper.insert(p);
            log.info("平台开发者账号已初始化: admin / 2768（预留手机号默认 15587571721）");
        } else if (!"admin".equals(dev.getUsername()) || !"2768".equals(dev.getPassword())) {
            // 旧库 dev 账号统一升级为 admin / 2768（不覆盖已配置的预留手机号）
            dev.setUsername("admin");
            dev.setPassword("2768");
            adminMapper.updateById(dev);
            log.info("平台开发者账号已升级为 admin / 2768");
        }
    }

    private record RoomSeed(String name, int price, int stock) { }

    private RoomSeed room(String name, int price, int stock) {
        return new RoomSeed(name, price, stock);
    }

    private void insertHouse(String name, String type, String area, String address,
                             double score, int reviews, int price, String desc, String tags, String prompt,
                             List<RoomSeed> rooms) {
        House h = new House();
        h.setName(name);
        h.setType(type);
        h.setArea(area);
        h.setAddress(address);
        h.setScore(score);
        h.setReviews(reviews);
        h.setPrice(java.math.BigDecimal.valueOf(price));
        h.setDescription(desc);
        h.setTags(tags);
        h.setImgPrompt(prompt);
        h.setStatus(1);
        houseMapper.insert(h);

        for (RoomSeed rs : rooms) {
            Room r = new Room();
            r.setHouseId(h.getId());
            r.setName(rs.name());
            r.setPrice(java.math.BigDecimal.valueOf(rs.price()));
            r.setStock(rs.stock());
            roomMapper.insert(r);
        }
    }
}
