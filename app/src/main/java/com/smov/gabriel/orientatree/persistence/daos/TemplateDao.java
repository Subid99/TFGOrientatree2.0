package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.smov.gabriel.orientatree.persistence.entities.OfflineTemplate;

import java.util.List;

// TemplateDao.java
@Dao
public interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTemplate(OfflineTemplate template);

    @Update
    void updateTemplate(OfflineTemplate template);

    @Delete
    void deleteTemplate(OfflineTemplate template);

    @Query("SELECT * FROM templates WHERE templateId = :templateId")
    OfflineTemplate getTemplateById(String templateId);

    @Query("SELECT * FROM templates")
    List<OfflineTemplate> getAllTemplates();

    @Query("DELETE FROM templates WHERE templateId = :templateId")
    void deleteTemplateById(String templateId);

    @Query("SELECT * FROM templates WHERE name LIKE '%' || :searchTerm || '%' OR description LIKE '%' || :searchTerm || '%'")
    List<OfflineTemplate> searchTemplates(String searchTerm);
}