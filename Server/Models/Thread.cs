using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Server.Models
{
    public partial class Thread
    {
        public long Id { get; set; }
        public long DemoId { get; set; }
        public long Pos { get; set; }
        public string Code { get; set; }

        [JsonIgnore]
        public virtual Demo Demo { get; set; }
    }
}
