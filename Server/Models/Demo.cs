using System;
using System.Collections.Generic;
using Microsoft.AspNetCore.Identity;

namespace Server.Models
{
    public partial class Demo
    {
        public const int PRIVATE = 0;
        public const int PUBLIC = 1;

        public Demo()
        {
            DemoTag = new HashSet<DemoTag>();
            Playback = new HashSet<Playback>();
            Thread = new HashSet<Thread>();
        }

        public long Id { get; set; }
        public string Title { get; set; }
        public string AuthorId { get; set; }
        public long Visibility { get; set; }
        public string Description { get; set; }
        public DateTime CreatedAt { get; set; }
        public DateTime UpdatedAt { get; set; }

        public virtual IdentityUser Author { get; set; }
        public virtual ICollection<DemoTag> DemoTag { get; set; }
        public virtual ICollection<Playback> Playback { get; set; }
        public virtual ICollection<Thread> Thread { get; set; }
    }
}
