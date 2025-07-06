package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Update;

import com.smov.gabriel.orientatree.persistence.entities.OfflineUser;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(OfflineUser user);

    @Update
    void updateUser(OfflineUser user);

    @Delete
    void deleteUser(OfflineUser user);

    @Query("SELECT * FROM users WHERE id = :userId")
    OfflineUser getUserById(String userId);

    @Query("SELECT * FROM users WHERE email = :email")
    OfflineUser getUserByEmail(String email);

    @Query("SELECT * FROM users")
    List<OfflineUser> getAllUsers();

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteUserById(String userId);

    @Query("UPDATE users SET name = :name, surname = :surname WHERE id = :userId")
    void updateUserName(String userId, String name, String surname);

    @Query("UPDATE users SET has_photo = :hasPhoto WHERE id = :userId")
    void updateUserPhotoStatus(String userId, boolean hasPhoto);
}
