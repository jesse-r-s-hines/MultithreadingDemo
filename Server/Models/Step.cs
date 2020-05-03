using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Server.Models
{
    public partial class Step
    {
        public long Id { get; set; }
        public long PlaybackId { get; set; }
        public long ThreadPos { get; set; }
        public long? WakeThreadPos { get; set; }
        public long Pos { get; set; }
        public bool Breakpoint { get; set; }

        [JsonIgnore]
        public virtual Playback Playback { get; set; }
    }
}
