package com.smov.gabriel.orientatree.persistence.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class OfflineUser {
        @PrimaryKey
        @NonNull
        public String id;

        public String name;
        public String surname;
        public String email;

        @ColumnInfo(name = "has_photo")
        public boolean hasPhoto;
}
