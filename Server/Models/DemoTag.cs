using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Server.Models
{
    public partial class DemoTag
    {
        public long DemoId { get; set; }
        public string TagName { get; set; }

        [JsonIgnore]
        public virtual Demo Demo { get; set; }
        [JsonIgnore]
        public virtual Tag TagNameNavigation { get; set; }
    }
}
