using System;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.EntityFrameworkCore;

namespace Server.Models
{
    public partial class MultithreadingContext : IdentityDbContext<IdentityUser>
    {
        public MultithreadingContext()
        {
        }

        public MultithreadingContext(DbContextOptions<MultithreadingContext> options)
            : base(options)
        {
        }

        public virtual DbSet<Demo> Demo { get; set; }
        public virtual DbSet<DemoTag> DemoTag { get; set; }
        public virtual DbSet<Playback> Playback { get; set; }
        public virtual DbSet<Step> Step { get; set; }
        public virtual DbSet<Tag> Tag { get; set; }
        public virtual DbSet<Thread> Thread { get; set; }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            // if (!optionsBuilder.IsConfigured)
            // {
            //     // #warning To protect potentially sensitive information in your connection string, you should move it out of source code. See http://go.microsoft.com/fwlink/?LinkId=723263 for guidance on storing connection strings.
            //     optionsBuilder.UseSqlite("Data Source=Server.db");
            // }
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);
            
            modelBuilder.Entity<Demo>(entity =>
            {
                entity.ToTable("demo");

                entity.Property(e => e.Id)
                    .HasColumnName("id");
                    // .ValueGeneratedNever();

                entity.Property(e => e.Title)
                    .IsRequired()
                    .HasColumnName("title")
                    .HasColumnType("varchar(255)");

                entity.Property(e => e.AuthorId)
                    .IsRequired()
                    .HasColumnName("author_id")
                    .HasColumnType("varchar(40)");

                entity.Property(e => e.Visibility)
                    .HasColumnName("visibility")
                    .HasColumnType("smallint");

                entity.Property(e => e.CreatedAt)
                    .IsRequired()
                    .HasColumnName("created_at")
                    .HasColumnType("datetime");

                entity.Property(e => e.UpdatedAt)
                    .IsRequired()
                    .HasColumnName("updated_at")
                    .HasColumnType("datetime");

                entity.Property(e => e.Description)
                    .HasColumnName("description");

                entity.HasOne(d => d.Author)
                    .WithMany()
                    .HasForeignKey(d => d.AuthorId);
            });

            modelBuilder.Entity<DemoTag>(entity =>
            {
                entity.HasKey(e => new { e.DemoId, e.TagName });

                entity.ToTable("demo_tag");

                entity.Property(e => e.DemoId).HasColumnName("demo_id");

                entity.Property(e => e.TagName)
                    .HasColumnName("tag_name")
                    .HasColumnType("varchar(255)");

                entity.HasOne(d => d.Demo)
                    .WithMany(p => p.DemoTag)
                    .HasForeignKey(d => d.DemoId);

                entity.HasOne(d => d.TagNameNavigation)
                    .WithMany(p => p.DemoTag)
                    .HasForeignKey(d => d.TagName);
            });

            modelBuilder.Entity<Playback>(entity =>
            {
                entity.ToTable("playback");

                entity.Property(e => e.Id)
                    .HasColumnName("id");
                    // .ValueGeneratedNever();

                entity.Property(e => e.DemoId).HasColumnName("demo_id");

                entity.Property(e => e.Name)
                    .IsRequired()
                    .HasColumnName("name")
                    .HasColumnType("varchar(255)");

                entity.HasOne(d => d.Demo)
                    .WithMany(p => p.Playback)
                    .HasForeignKey(d => d.DemoId);
            });

            modelBuilder.Entity<Step>(entity =>
            {
                entity.ToTable("step");

                entity.Property(e => e.Id)
                    .HasColumnName("id");
                    // .ValueGeneratedNever();

                entity.Property(e => e.PlaybackId).HasColumnName("playback_id");

                entity.Property(e => e.Pos).HasColumnName("pos");

                entity.Property(e => e.ThreadPos).HasColumnName("thread_pos");

                entity.Property(e => e.WakeThreadPos).HasColumnName("wake_thread_pos");

                entity.Property(e => e.Breakpoint)
                    .HasColumnName("breakpoint")
                    .HasColumnType("smallint");

                entity.HasOne(d => d.Playback)
                    .WithMany(p => p.Step)
                    .HasForeignKey(d => d.PlaybackId);
            });

            modelBuilder.Entity<Tag>(entity =>
            {
                entity.HasKey(e => e.Name);

                entity.ToTable("tag");

                entity.Property(e => e.Name)
                    .HasColumnName("name")
                    .HasColumnType("varchar(255)");
            });

            modelBuilder.Entity<Thread>(entity =>
            {
                entity.ToTable("thread");

                entity.HasIndex(e => new { e.DemoId, e.Pos })
                    .IsUnique();

                entity.Property(e => e.Id)
                    .HasColumnName("id");
                    // .ValueGeneratedNever();

                entity.Property(e => e.Code)
                    .IsRequired()
                    .HasColumnName("code");

                entity.Property(e => e.DemoId).HasColumnName("demo_id");

                entity.Property(e => e.Pos).HasColumnName("pos");

                entity.HasOne(d => d.Demo)
                    .WithMany(p => p.Thread)
                    .HasForeignKey(d => d.DemoId);
            });

            OnModelCreatingPartial(modelBuilder);
        }

        partial void OnModelCreatingPartial(ModelBuilder modelBuilder);
    }
}
