package edu.cit.antolijao.channel;

import jakarta.persistence.*;

@Entity
@Table(name = "channel_cursor")
public class ChannelCursor {

    @Id
    private Integer id;

    @Column(name = "last_seq", nullable = false)
    private Long lastSeq;

    public ChannelCursor() {}

    public Integer getId() { return id; }
    public Long getLastSeq() { return lastSeq; }
    public void setLastSeq(Long lastSeq) { this.lastSeq = lastSeq; }
}