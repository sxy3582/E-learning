-- 项目初始化演示数据：仅建议在空库初始化时执行一次

INSERT INTO sys_user(id, username, password, nickname, role, banned, create_time, update_time)
VALUES (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '系统管理员', 'ADMIN', 0, NOW(), NOW());

INSERT INTO course(id, title, intro, cover_url, difficulty, category, published, sort_order, create_time, update_time)
VALUES (11001, 'Java 入门到实践', '面向新手的 Java 基础课程', '', '初级', '后端开发', 1, 1, NOW(), NOW()),
       (11002, '算法思维训练', '包含排序、查找和复杂度核心思想', '', '中级', '算法', 1, 2, NOW(), NOW()),
       (11003, '数据结构基础', '以数组、链表、栈队列建立结构化思维', '', '初级', '计算机基础', 1, 3, NOW(), NOW());

INSERT INTO chapter(id, course_id, title, content_type, content_value, sort_order, create_time, update_time)
VALUES
    (12001, 11001, 'Java 环境与基础语法', 'article', '本章介绍 Java 环境搭建、变量、流程控制和基础语法。', 1, NOW(), NOW()),
    (12002, 11001, '面向对象核心概念', 'video', 'https://player.bilibili.com/player.html?bvid=BV17F411T7Ao&page=1&high_quality=1&danmaku=0&autoplay=0', 2, NOW(), NOW()),
    (12003, 11001, 'Java 基础练习', 'question', '/practice/11001/12003', 3, NOW(), NOW()),
    (12004, 11001, 'Java 编程练习入口', 'code_problem', '/code-practice?problem=15001', 4, NOW(), NOW()),
    (12005, 11001, '排序算法实验入口', 'algorithm_lab', '/algorithm-lab?algo=bubble', 5, NOW(), NOW()),

    (12006, 11002, '时间复杂度与空间复杂度', 'article', '理解 O(1)、O(logn)、O(n)、O(nlogn) 的常见场景。', 1, NOW(), NOW()),
    (12007, 11002, '二分查找思路讲解', 'video', 'https://player.bilibili.com/player.html?bvid=BV1fA4y1o715&page=1&high_quality=1&danmaku=0&autoplay=0', 2, NOW(), NOW()),
    (12008, 11002, '算法基础练习', 'question', '/practice/11002/12008', 3, NOW(), NOW()),
    (12009, 11002, '算法编程练习入口', 'code_problem', '/code-practice?problem=15002', 4, NOW(), NOW()),
    (12010, 11002, '二分查找实验入口', 'algorithm_lab', '/algorithm-lab?algo=binary', 5, NOW(), NOW()),

    (12011, 11003, '数组与链表基础', 'article', '对比顺序存储与链式存储，理解插入删除和访问复杂度。', 1, NOW(), NOW()),
    (12012, 11003, '栈与队列应用', 'video', 'https://player.bilibili.com/player.html?bvid=BV1y7411d774&page=1&high_quality=1&danmaku=0&autoplay=0', 2, NOW(), NOW()),
    (12013, 11003, '数据结构练习', 'question', '/practice/11003/12013', 3, NOW(), NOW()),
    (12014, 11003, '结构编程练习入口', 'code_problem', '/code-practice?problem=15003', 4, NOW(), NOW()),
    (12015, 11003, '选择排序实验入口', 'algorithm_lab', '/algorithm-lab?algo=selection', 5, NOW(), NOW()),
    (12016, 11002, '插入排序实验入口', 'algorithm_lab', '/algorithm-lab?algo=insertion', 6, NOW(), NOW()),
    (12017, 11003, '线性查找实验入口', 'algorithm_lab', '/algorithm-lab?algo=linear', 6, NOW(), NOW());

INSERT INTO question(id, chapter_id, type, stem, analysis, reference_answer, sort_order, create_time, update_time)
VALUES
    (13001, 12003, 'SINGLE', 'Java 中用于输出到控制台的语句是？', 'System.out.println() 是标准输出方法。', 'B', 1, NOW(), NOW()),
    (13002, 12003, 'JUDGE', 'Java 是强类型语言。', 'Java 在编译阶段会进行类型检查。', 'TRUE', 2, NOW(), NOW()),
    (13003, 12003, 'JUDGE', 'JDK 1.8 可以直接用于 Java 开发。', 'JDK 1.8 包含编译与运行环境。', 'TRUE', 3, NOW(), NOW()),

    (13004, 12008, 'SINGLE', '二分查找适用于哪种数据集合？', '二分查找要求数据有序。', 'A', 1, NOW(), NOW()),
    (13005, 12008, 'JUDGE', '二分查找每次会把搜索区间缩小一半。', '这正是二分查找效率高的原因。', 'TRUE', 2, NOW(), NOW()),
    (13006, 12008, 'JUDGE', '线性查找在有序数组中一定比二分查找快。', '通常不是，二分查找更快。', 'FALSE', 3, NOW(), NOW()),

    (13007, 12013, 'SINGLE', '链表相比数组的主要优势之一是？', '链表插入删除在已知节点位置时更灵活。', 'C', 1, NOW(), NOW()),
    (13008, 12013, 'JUDGE', '栈是先进后出（LIFO）结构。', '栈遵循后进先出原则。', 'TRUE', 2, NOW(), NOW()),
    (13009, 12013, 'JUDGE', '队列是后进先出（LIFO）结构。', '队列应是先进先出（FIFO）。', 'FALSE', 3, NOW(), NOW());

INSERT INTO question_option(id, question_id, option_key, option_content, is_correct, sort_order, create_time, update_time)
VALUES
    (14001, 13001, 'A', 'System.print()', 0, 1, NOW(), NOW()),
    (14002, 13001, 'B', 'System.out.println()', 1, 2, NOW(), NOW()),
    (14003, 13001, 'C', 'Console.write()', 0, 3, NOW(), NOW()),
    (14004, 13001, 'D', 'print.console()', 0, 4, NOW(), NOW()),

    (14005, 13004, 'A', '有序数组', 1, 1, NOW(), NOW()),
    (14006, 13004, 'B', '无序链表', 0, 2, NOW(), NOW()),
    (14007, 13004, 'C', '无序哈希表', 0, 3, NOW(), NOW()),
    (14008, 13004, 'D', '任意集合都行', 0, 4, NOW(), NOW()),

    (14009, 13007, 'A', '随机访问速度恒定', 0, 1, NOW(), NOW()),
    (14010, 13007, 'B', '内存连续，缓存友好', 0, 2, NOW(), NOW()),
    (14011, 13007, 'C', '插入删除时不需要整体搬移元素', 1, 3, NOW(), NOW()),
    (14012, 13007, 'D', '一定比数组更省内存', 0, 4, NOW(), NOW());

INSERT INTO code_problem(id, title, description, method_name, template_code, sort_order, create_time, update_time)
VALUES
    (15001, '两数之和', '给定两个整数 a 和 b，返回它们的和。', 'add',
     'public class Solution {\n    public int add(int a, int b) {\n        return 0;\n    }\n}', 1, NOW(), NOW()),
    (15002, '两数较大值', '给定两个整数，返回较大的那个值。', 'max',
     'public class Solution {\n    public int max(int a, int b) {\n        return 0;\n    }\n}', 2, NOW(), NOW()),
    (15003, '偶数判断', '给定整数 n，若为偶数返回 1，否则返回 0。', 'isEven',
     'public class Solution {\n    public int isEven(int n) {\n        return 0;\n    }\n}', 3, NOW(), NOW()),
    (15004, '斐波那契数', '给定非负整数 n，返回第 n 个斐波那契数。约定 f(0)=0，f(1)=1。', 'fib',
     'public class Solution {\n    public int fib(int n) {\n        return 0;\n    }\n}', 4, NOW(), NOW()),
    (15005, '最大公约数', '给定两个正整数 a 和 b，返回它们的最大公约数。', 'gcd',
     'public class Solution {\n    public int gcd(int a, int b) {\n        return 0;\n    }\n}', 5, NOW(), NOW()),
    (15006, '阶乘计算', '给定整数 n，返回 n 的阶乘。测试数据保证结果在 int 范围内。', 'factorial',
     'public class Solution {\n    public int factorial(int n) {\n        return 0;\n    }\n}', 6, NOW(), NOW()),
    (15007, '数字各位求和', '给定一个非负整数 n，返回它每一位数字之和。', 'digitSum',
     'public class Solution {\n    public int digitSum(int n) {\n        return 0;\n    }\n}', 7, NOW(), NOW()),
    (15008, '绝对差值', '给定两个整数 a 和 b，返回它们差值的绝对值。', 'absDiff',
     'public class Solution {\n    public int absDiff(int a, int b) {\n        return 0;\n    }\n}', 8, NOW(), NOW());

INSERT INTO code_test_case(id, problem_id, input_json, expected_output, is_sample, sort_order, create_time, update_time)
VALUES
    (16001, 15001, '[1,2]', '3', 1, 1, NOW(), NOW()),
    (16002, 15001, '[10,20]', '30', 1, 2, NOW(), NOW()),
    (16003, 15001, '[-5,8]', '3', 0, 3, NOW(), NOW()),
    (16004, 15002, '[8,3]', '8', 1, 1, NOW(), NOW()),
    (16005, 15002, '[-1,7]', '7', 0, 2, NOW(), NOW()),
    (16006, 15002, '[9,9]', '9', 0, 3, NOW(), NOW()),
    (16007, 15003, '[2]', '1', 1, 1, NOW(), NOW()),
    (16008, 15003, '[3]', '0', 1, 2, NOW(), NOW()),
    (16009, 15003, '[100]', '1', 0, 3, NOW(), NOW()),
    (16010, 15004, '[0]', '0', 1, 1, NOW(), NOW()),
    (16011, 15004, '[1]', '1', 1, 2, NOW(), NOW()),
    (16012, 15004, '[7]', '13', 0, 3, NOW(), NOW()),
    (16013, 15005, '[12,18]', '6', 1, 1, NOW(), NOW()),
    (16014, 15005, '[17,13]', '1', 0, 2, NOW(), NOW()),
    (16015, 15005, '[100,25]', '25', 0, 3, NOW(), NOW()),
    (16016, 15006, '[0]', '1', 1, 1, NOW(), NOW()),
    (16017, 15006, '[5]', '120', 1, 2, NOW(), NOW()),
    (16018, 15006, '[7]', '5040', 0, 3, NOW(), NOW()),
    (16019, 15007, '[123]', '6', 1, 1, NOW(), NOW()),
    (16020, 15007, '[1008]', '9', 0, 2, NOW(), NOW()),
    (16021, 15007, '[0]', '0', 0, 3, NOW(), NOW()),
    (16022, 15008, '[10,3]', '7', 1, 1, NOW(), NOW()),
    (16023, 15008, '[-2,5]', '7', 0, 2, NOW(), NOW()),
    (16024, 15008, '[4,4]', '0', 0, 3, NOW(), NOW());
