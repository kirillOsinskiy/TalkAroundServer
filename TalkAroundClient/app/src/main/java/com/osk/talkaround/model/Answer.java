package com.osk.talkaround.model;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

/**
 * Created by KOsinsky on 19.03.2016.
 */
public class Answer implements Serializable, Comparable {
    private static final long serialVersionUID = -30525038118543965L;
    private BigInteger id;
    private BigInteger talkId;
    private long orderNumber;
    private Date answerDate;
    private String message;
    private String attachment;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    public BigInteger getTalkId() {
        return talkId;
    }

    public void setTalkId(BigInteger talkId) {
        this.talkId = talkId;
    }

    public long getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(long orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Date getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(Date answerDate) {
        this.answerDate = answerDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Answer answer = (Answer) o;

        if (!id.equals(answer.id)) return false;
        return talkId.equals(answer.talkId);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + talkId.hashCode();
        return result;
    }

    public int compareTo(Object o) {
        return answerDate.compareTo(((Answer)o).getAnswerDate());
    }
}
