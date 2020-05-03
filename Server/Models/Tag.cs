using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Server.Models
{
    public partial class Tag
    {
        public Tag()
        {
            DemoTag = new HashSet<DemoTag>();
        }

        public string Name { get; set; }

        [JsonIgnore]
        public virtual ICollection<DemoTag> DemoTag { get; set; }
    }
}
