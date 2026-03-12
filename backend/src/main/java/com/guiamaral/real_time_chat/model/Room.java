package com.guiamaral.real_time_chat.model;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("rooms")
public class Room implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	@NotBlank(message = "name is required")
	private String name;

	@Indexed
	@NotBlank(message = "code is required")
	private String code;

	@NotBlank(message = "ownerId is required")
	private String ownerId;

	private Set<String> memberIds = new LinkedHashSet<>();

	public Room() {
	}

	public Room(String id, String name, String code, String ownerId, Set<String> memberIds) {
		this.id = id;
		this.name = name;
		this.code = code;
		this.ownerId = ownerId;
		this.memberIds = memberIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(memberIds);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public Set<String> getMemberIds() {
		return memberIds;
	}

	public void setMemberIds(Set<String> memberIds) {
		this.memberIds = memberIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(memberIds);
	}

	@Override
	public String toString() {
		return "Room{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				", code='" + code + '\'' +
				", ownerId='" + ownerId + '\'' +
				", memberIds=" + memberIds +
				'}';
	}
}
