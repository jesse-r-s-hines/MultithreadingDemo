using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Server.Migrations
{
    public partial class Seed : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // TODO! Seed data in a better way than this hacky SQL dump.
            // Also, we're hardcoding the initial password here. Obviously you should change it if you host this.
            migrationBuilder.Sql(@"
-- Populate Tags
INSERT INTO tag(name) VALUES
    ('Mutexes'), ('Condition Variables'), ('Semaphores'), ('Race Conditions'), ('Deadlock');

-- Populate with example data.
INSERT INTO 'AspNetUsers' VALUES
    ('71398e3b-38f2-46fd-87c1-f8877294aa5f','demo','Demo',NULL,NULL,0,'AQAAAAEAACcQAAAAEM6x8I2ss38EOXDdVnjx+aPiv1aOyAPwMSRfiptq433I/46r22ZONU6S90jof033xA==','Z7SVNPOCEOAMYVUJZ6ZSP6NV7IWO2OYK','2ae7aba0-2749-44c7-ad24-2d67fe3f9fd2',NULL,0,0,NULL,1,0);

INSERT INTO 'demo' VALUES
    (1,'Counter','71398e3b-38f2-46fd-87c1-f8877294aa5f',1,'A basic counter that shows race conditions.','2020-05-03 13:28:01.8833006','2020-05-03 13:28:01.8833511'),
    (2,'Counter with Mutex','71398e3b-38f2-46fd-87c1-f8877294aa5f',1,'The counter, but with locks to prevent the race condition.','2020-05-03 13:29:24.734452','2020-05-03 13:29:24.7344894'),
    (3,'Condition Variables','71398e3b-38f2-46fd-87c1-f8877294aa5f',1,'Basic condition variable example.','2020-05-03 13:37:11.5560877','2020-05-03 13:37:11.5560938'),
    (4,'Binary Semaphore','71398e3b-38f2-46fd-87c1-f8877294aa5f',1,'Example of using a semaphore as a mutex.','2020-05-03 13:40:53.728621','2020-05-03 13:40:53.7286243'),
    (5,'Semaphores as Condition Variables','71398e3b-38f2-46fd-87c1-f8877294aa5f',1,'Example of using a semaphore as a condition variable.','2020-05-03 13:41:51.9393543','2020-05-03 13:41:51.9393651'),
    (6,'Deadlock','71398e3b-38f2-46fd-87c1-f8877294aa5f',1,'Demonstrates deadlock.','2020-05-03 13:52:37.0341788','2020-05-03 13:52:37.0342408');

INSERT INTO 'demo_tag' VALUES
    (1,'Race Conditions'),
    (2,'Mutexes'),
    (3,'Condition Variables'),
    (4,'Semaphores'),
    (5,'Semaphores'),
    (6,'Deadlock');

INSERT INTO 'thread' VALUES
(1,1,0,
'// We''d expect the counter to end up being 6 once both threads are complete, but the actual result
// can vary.
int counter = 0;'),
(2,1,1,
'for (int i = 0; i < 3; i++) {
    counter++;
}'),
(3,1,2,
'for (int i = 0; i < 3; i++) {
    counter++;
}'),
(4,2,1,
'for (int i = 0; i < 3; i++) {
    lock(m);
        counter++;
    unlock(m);
}'),
(5,2,0,
'int counter = 0;
mutex m;'),
(6,2,2,
'for (int i = 0; i < 3; i++) {
    lock(m);
        counter++;
    unlock(m);
}'),
(7,3,0,
'bool done = false;
cond_var c;'),
(8,3,1,
'// This thread will wait until
//the other thread has finished.
cond_wait(c);
done = true;
'),
(9,3,2,
'// do some stuff...
for (int i = 0; i < 2; i++) {}
// Signal the other thread that we''re done.
cond_signal(c);'),
(10,4,0,
'int counter = 0;
sem s = sem_create(1);'),
(11,4,1,
'for (int i = 0; i < 3; i++) {
    sem_wait(s);
        counter++;
    sem_post(s);
}'),
(12,4,2,
'for (int i = 0; i < 3; i++) {
    sem_wait(s);
        counter++;
    sem_post(s);
}'),
(13,5,2,
'// do some stuff...
for (int i = 0; i < 2; i++) {}
// Signal the other thread that we''re done.
sem_post(s);'),
(14,5,0,
'bool done = false;
sem s = sem_create(0);
'),
(15,5,1,
'// This thread will wait until
//the other thread has finished.
sem_wait(s);
done = true;
'),
(16,6,0,
'int counter = 0;
mutex m0; 
mutex m1;'),
(17,6,1,
'for (int i = 0; i < 3; i++) {
    lock(m0);
    lock(m1);
        counter++;
    unlock(m1);
    unlock(m0);
}'),
(18,6,2,
'for (int i = 0; i < 3; i++) {
    lock(m1);
    lock(m0);
        counter++;
    unlock(m0);
    unlock(m1);
}');

INSERT INTO 'playback' VALUES
    (3,2,'Successful Run'),
    (4,1,'Race Condition'),
    (6,3,'Condition Variable Example'),
    (7,4,'Binary Semaphore Example'),
    (8,5,'Sem as Condition Variable Example'),
    (9,6,'Deadlock Example');

INSERT INTO 'step' VALUES
    (1,3,1,NULL,53,0),
    (2,3,1,NULL,52,0),
    (3,3,1,NULL,51,0),
    (4,3,1,NULL,50,0),
    (5,3,1,NULL,49,0),
    (6,3,1,NULL,48,0),
    (7,3,1,NULL,47,0),
    (8,3,1,NULL,46,0),
    (9,3,1,NULL,45,0),
    (10,3,1,NULL,44,0),
    (11,3,1,NULL,43,0),
    (12,3,1,NULL,42,0),
    (13,3,1,NULL,41,0),
    (14,3,1,NULL,40,0),
    (15,3,1,NULL,39,0),
    (16,3,1,NULL,54,0),
    (17,3,1,NULL,38,0),
    (18,3,1,NULL,55,0),
    (19,3,1,NULL,57,0),
    (20,3,2,NULL,72,0),
    (21,3,2,NULL,71,0),
    (22,3,2,NULL,70,0),
    (23,3,2,NULL,69,0),
    (24,3,2,NULL,68,0),
    (25,3,2,NULL,67,0),
    (26,3,2,NULL,66,0),
    (27,3,2,NULL,65,0),
    (28,3,2,NULL,64,0),
    (29,3,2,NULL,63,0),
    (30,3,2,NULL,62,0),
    (31,3,2,NULL,61,0),
    (32,3,2,NULL,60,0),
    (33,3,2,NULL,59,0),
    (34,3,2,NULL,58,0),
    (35,3,1,NULL,56,0),
    (36,3,2,NULL,73,0),
    (37,3,1,NULL,37,0),
    (38,3,2,NULL,35,0),
    (39,3,2,NULL,15,0),
    (40,3,2,NULL,14,0),
    (41,3,2,NULL,13,0),
    (42,3,1,NULL,12,0),
    (43,3,1,NULL,11,0),
    (44,3,2,NULL,10,0),
    (45,3,2,NULL,9,0),
    (46,3,2,NULL,8,0),
    (47,3,2,NULL,7,0),
    (48,3,1,NULL,6,0),
    (49,3,1,NULL,5,0),
    (50,3,1,NULL,4,0),
    (51,3,1,NULL,3,0),
    (52,3,1,NULL,2,0),
    (53,3,1,NULL,1,0),
    (54,3,2,NULL,16,0),
    (55,3,1,NULL,36,0),
    (56,3,2,NULL,17,0),
    (57,3,2,NULL,19,0),
    (58,3,2,NULL,34,0),
    (59,3,2,NULL,33,0),
    (60,3,2,NULL,32,0),
    (61,3,2,NULL,31,0),
    (62,3,1,NULL,30,0),
    (63,3,1,NULL,29,0),
    (64,3,1,NULL,28,0),
    (65,3,1,NULL,27,0),
    (66,3,1,NULL,26,0),
    (67,3,1,NULL,25,0),
    (68,3,2,NULL,24,0),
    (69,3,2,NULL,23,0),
    (70,3,2,NULL,22,0),
    (71,3,2,NULL,21,0),
    (72,3,2,NULL,20,0),
    (73,3,2,NULL,18,0),
    (74,3,1,NULL,0,0),

    (75,4,2,NULL,32,0),
    (76,4,2,NULL,33,0),
    (77,4,2,NULL,34,0),
    (78,4,1,NULL,35,1),
    (79,4,1,NULL,36,0),
    (80,4,1,NULL,37,0),
    (81,4,1,NULL,38,0),
    (82,4,1,NULL,39,0),
    (83,4,1,NULL,40,0),
    (84,4,1,NULL,41,0),
    (85,4,1,NULL,42,0),
    (86,4,1,NULL,43,0),
    (87,4,1,NULL,44,0),
    (88,4,1,NULL,45,0),
    (89,4,1,NULL,46,0),
    (90,4,1,NULL,47,0),
    (91,4,1,NULL,48,0),
    (92,4,1,NULL,49,0),
    (93,4,1,NULL,50,0),
    (94,4,1,NULL,51,0),
    (95,4,1,NULL,52,0),
    (96,4,1,NULL,53,0),
    (97,4,1,NULL,54,0),
    (98,4,1,NULL,55,0),
    (99,4,1,NULL,56,0),
    (100,4,1,NULL,57,0),
    (101,4,1,NULL,58,0),
    (102,4,2,NULL,31,0),
    (103,4,1,NULL,59,0),
    (104,4,2,NULL,30,0),
    (105,4,2,NULL,28,0),
    (106,4,1,NULL,1,0),
    (107,4,1,NULL,2,0),
    (108,4,1,NULL,3,0),
    (109,4,1,NULL,4,0),
    (110,4,2,NULL,5,0),
    (111,4,2,NULL,6,0),
    (112,4,2,NULL,7,0),
    (113,4,2,NULL,8,0),
    (114,4,2,NULL,9,0),
    (115,4,2,NULL,10,0),
    (116,4,2,NULL,11,0),
    (117,4,2,NULL,12,0),
    (118,4,2,NULL,13,0),
    (119,4,2,NULL,14,0),
    (120,4,2,NULL,15,0),
    (121,4,2,NULL,16,0),
    (122,4,2,NULL,17,0),
    (123,4,2,NULL,18,0),
    (124,4,2,NULL,19,0),
    (125,4,2,NULL,20,0),
    (126,4,2,NULL,21,0),
    (127,4,2,NULL,22,0),
    (128,4,2,NULL,23,0),
    (129,4,2,NULL,24,0),
    (130,4,2,NULL,25,0),
    (131,4,2,NULL,26,0),
    (132,4,2,NULL,27,0),
    (133,4,2,NULL,29,0),
    (134,4,1,NULL,0,0),

    (135,6,1,NULL,16,0),
    (136,6,1,NULL,15,0),
    (137,6,2,NULL,14,0),
    (138,6,2,NULL,13,0),
    (139,6,2,NULL,12,0),
    (140,6,2,NULL,11,0),
    (141,6,2,NULL,10,0),
    (142,6,1,NULL,17,0),
    (143,6,2,NULL,9,0),
    (144,6,2,NULL,7,0),
    (145,6,2,NULL,6,0),
    (146,6,2,NULL,5,0),
    (147,6,2,NULL,4,0),
    (148,6,2,NULL,3,0),
    (149,6,2,NULL,2,0),
    (150,6,2,NULL,1,0),
    (151,6,2,NULL,8,0),
    (152,6,1,NULL,0,0),

    (153,7,1,NULL,0,0),
    (154,7,1,NULL,51,0),
    (155,7,1,NULL,50,0),
    (156,7,1,NULL,49,0),
    (157,7,1,NULL,48,0),
    (158,7,1,NULL,47,0),
    (159,7,1,NULL,46,0),
    (160,7,1,NULL,52,0),
    (161,7,1,NULL,45,0),
    (162,7,1,NULL,43,0),
    (163,7,1,NULL,42,0),
    (164,7,1,NULL,41,0),
    (165,7,1,NULL,40,0),
    (166,7,1,NULL,39,0),
    (167,7,1,NULL,38,0),
    (168,7,1,NULL,44,0),
    (169,7,1,NULL,37,0),
    (170,7,1,NULL,53,0),
    (171,7,1,NULL,55,0),
    (172,7,2,NULL,69,0),
    (173,7,2,NULL,68,0),
    (174,7,2,NULL,67,0),
    (175,7,2,NULL,66,0),
    (176,7,2,NULL,65,0),
    (177,7,2,NULL,64,0),
    (178,7,1,NULL,54,0),
    (179,7,2,NULL,63,0),
    (180,7,2,NULL,61,0),
    (181,7,2,NULL,60,0),
    (182,7,2,NULL,59,0),
    (183,7,2,NULL,58,0),
    (184,7,2,NULL,57,0),
    (185,7,2,NULL,56,0),
    (186,7,2,NULL,62,0),
    (187,7,1,NULL,36,0),
    (188,7,1,NULL,35,0),
    (189,7,2,1,34,0),
    (190,7,2,NULL,14,0),
    (191,7,2,NULL,13,0),
    (192,7,1,2,12,0),
    (193,7,1,NULL,11,0),
    (194,7,1,NULL,10,0),
    (195,7,1,NULL,9,0),
    (196,7,2,NULL,15,0),
    (197,7,1,NULL,8,0),
    (198,7,2,NULL,6,0),
    (199,7,2,NULL,5,0),
    (200,7,2,NULL,4,0),
    (201,7,1,NULL,3,0),
    (202,7,1,NULL,2,0),
    (203,7,1,NULL,1,0),
    (204,7,2,NULL,7,0),
    (205,7,2,NULL,16,0),
    (206,7,2,NULL,17,0),
    (207,7,2,NULL,18,0),
    (208,7,2,NULL,33,0),
    (209,7,2,NULL,32,0),
    (210,7,2,NULL,31,0),
    (211,7,2,NULL,30,0),
    (212,7,1,NULL,29,0),
    (213,7,1,NULL,28,0),
    (214,7,1,NULL,27,0),
    (215,7,1,NULL,26,0),
    (216,7,1,NULL,25,0),
    (217,7,1,NULL,24,0),
    (218,7,2,NULL,23,0),
    (219,7,2,NULL,22,0),
    (220,7,2,NULL,21,0),
    (221,7,2,NULL,20,0),
    (222,7,2,NULL,19,0),
    (223,7,2,NULL,70,0),
    (224,7,2,NULL,71,0),

    (225,8,1,NULL,0,0),
    (226,8,2,1,14,0),
    (227,8,2,NULL,13,0),
    (228,8,2,NULL,12,0),
    (229,8,2,NULL,11,0),
    (230,8,2,NULL,10,0),
    (231,8,2,NULL,9,0),
    (232,8,1,NULL,15,0),
    (233,8,2,NULL,8,0),
    (234,8,2,NULL,6,0),
    (235,8,2,NULL,5,0),
    (236,8,2,NULL,4,0),
    (237,8,2,NULL,3,0),
    (238,8,2,NULL,2,0),
    (239,8,2,NULL,1,0),
    (240,8,2,NULL,7,0),
    (241,8,1,NULL,16,0),

    (242,9,1,NULL,0,0),
    (243,9,1,NULL,1,0),
    (244,9,1,NULL,2,0),
    (245,9,1,NULL,3,0),
    (246,9,2,NULL,4,0),
    (247,9,2,NULL,5,0),
    (248,9,2,NULL,6,0),
    (249,9,2,NULL,7,0),
    (250,9,2,NULL,8,0),
    (251,9,1,NULL,9,0);
");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            
        }
    }
}
