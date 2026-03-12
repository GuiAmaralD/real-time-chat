package com.guiamaral.real_time_chat.repository;

import java.util.Optional;

import com.guiamaral.real_time_chat.model.Room;
import org.springframework.data.repository.CrudRepository;

public interface RoomRepository extends CrudRepository<Room, String> {
	Optional<Room> findByCode(String code);
}
