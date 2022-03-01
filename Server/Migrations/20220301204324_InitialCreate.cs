using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Server.Migrations
{
    public partial class InitialCreate : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "demo",
                columns: table => new
                {
                    id = table.Column<long>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    title = table.Column<string>(type: "varchar(255)", nullable: false),
                    author_id = table.Column<string>(type: "varchar(40)", nullable: false),
                    visibility = table.Column<long>(type: "smallint", nullable: false),
                    description = table.Column<string>(type: "TEXT", nullable: true),
                    created_at = table.Column<DateTime>(type: "datetime", nullable: false),
                    updated_at = table.Column<DateTime>(type: "datetime", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_demo", x => x.id);
                    table.ForeignKey(
                        name: "FK_demo_AspNetUsers_author_id",
                        column: x => x.author_id,
                        principalTable: "AspNetUsers",
                        principalColumn: "Id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "tag",
                columns: table => new
                {
                    name = table.Column<string>(type: "varchar(255)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_tag", x => x.name);
                });

            migrationBuilder.CreateTable(
                name: "playback",
                columns: table => new
                {
                    id = table.Column<long>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    demo_id = table.Column<long>(type: "INTEGER", nullable: false),
                    name = table.Column<string>(type: "varchar(255)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_playback", x => x.id);
                    table.ForeignKey(
                        name: "FK_playback_demo_demo_id",
                        column: x => x.demo_id,
                        principalTable: "demo",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "thread",
                columns: table => new
                {
                    id = table.Column<long>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    demo_id = table.Column<long>(type: "INTEGER", nullable: false),
                    pos = table.Column<long>(type: "INTEGER", nullable: false),
                    code = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_thread", x => x.id);
                    table.ForeignKey(
                        name: "FK_thread_demo_demo_id",
                        column: x => x.demo_id,
                        principalTable: "demo",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "demo_tag",
                columns: table => new
                {
                    demo_id = table.Column<long>(type: "INTEGER", nullable: false),
                    tag_name = table.Column<string>(type: "varchar(255)", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_demo_tag", x => new { x.demo_id, x.tag_name });
                    table.ForeignKey(
                        name: "FK_demo_tag_demo_demo_id",
                        column: x => x.demo_id,
                        principalTable: "demo",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                    table.ForeignKey(
                        name: "FK_demo_tag_tag_tag_name",
                        column: x => x.tag_name,
                        principalTable: "tag",
                        principalColumn: "name",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "step",
                columns: table => new
                {
                    id = table.Column<long>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    playback_id = table.Column<long>(type: "INTEGER", nullable: false),
                    thread_pos = table.Column<long>(type: "INTEGER", nullable: false),
                    wake_thread_pos = table.Column<long>(type: "INTEGER", nullable: true),
                    pos = table.Column<long>(type: "INTEGER", nullable: false),
                    breakpoint = table.Column<bool>(type: "smallint", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_step", x => x.id);
                    table.ForeignKey(
                        name: "FK_step_playback_playback_id",
                        column: x => x.playback_id,
                        principalTable: "playback",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_demo_author_id",
                table: "demo",
                column: "author_id");

            migrationBuilder.CreateIndex(
                name: "IX_demo_tag_tag_name",
                table: "demo_tag",
                column: "tag_name");

            migrationBuilder.CreateIndex(
                name: "IX_playback_demo_id",
                table: "playback",
                column: "demo_id");

            migrationBuilder.CreateIndex(
                name: "IX_step_playback_id",
                table: "step",
                column: "playback_id");

            migrationBuilder.CreateIndex(
                name: "IX_thread_demo_id_pos",
                table: "thread",
                columns: new[] { "demo_id", "pos" },
                unique: true);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "demo_tag");

            migrationBuilder.DropTable(
                name: "step");

            migrationBuilder.DropTable(
                name: "thread");

            migrationBuilder.DropTable(
                name: "tag");

            migrationBuilder.DropTable(
                name: "playback");

            migrationBuilder.DropTable(
                name: "demo");
        }
    }
}
