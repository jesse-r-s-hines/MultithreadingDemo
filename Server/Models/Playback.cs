using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Server.Models
{
    public partial class Playback
    {
        public Playback()
        {
            Step = new HashSet<Step>();
        }

        public long Id { get; set; }
        public long DemoId { get; set; }
        public string Name { get; set; }

        [JsonIgnore]
        public virtual Demo Demo { get; set; }
        public virtual ICollection<Step> Step { get; set; }
    }
}
