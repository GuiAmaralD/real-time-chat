package com.guiamaral.real_time_chat.model;

import java.time.Instant;

public class Message {

	private String id;
	private String roomId;
	private String userId;
	private String userNickname;
	private String content;
	private Instant sentAt;

	public Message() {
	}

	public Message(String id, String roomId, String userId, String content, Instant sentAt) {
		this(id, roomId, userId, null, content, sentAt);
	}

	public Message(String id, String roomId, String userId, String userNickname, String content, Instant sentAt) {
		this.id = id;
		this.roomId = roomId;
		this.userId = userId;
		this.userNickname = userNickname;
		this.content = content;
		this.sentAt = sentAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUserNickname() {
		return userNickname;
	}

	public void setUserNickname(String userNickname) {
		this.userNickname = userNickname;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}
}
