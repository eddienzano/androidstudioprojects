package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.aaagrowers.scoutingapp.data.database.entity.ScoreDefinitionEntity;

import java.util.List;

@Dao
public interface ScoreDefinitionDao {

    @Insert
    long insert(
            ScoreDefinitionEntity scoreDefinition
    );

    @Insert
    void insertAll(
            List<ScoreDefinitionEntity> scoreDefinitions
    );

    @Update
    void update(
            ScoreDefinitionEntity scoreDefinition
    );

    @Query(
            "SELECT * FROM score_definitions " +
                    "ORDER BY score ASC"
    )
    List<ScoreDefinitionEntity> getAll();

    @Query(
            "SELECT * FROM score_definitions " +
                    "WHERE active = 1 " +
                    "ORDER BY score ASC"
    )
    List<ScoreDefinitionEntity> getActive();

    @Query(
            "SELECT * FROM score_definitions " +
                    "WHERE id = :id " +
                    "LIMIT 1"
    )
    ScoreDefinitionEntity getById(int id);

    @Query(
            "SELECT * FROM score_definitions " +
                    "WHERE score = :score " +
                    "LIMIT 1"
    )
    ScoreDefinitionEntity getByScore(int score);

    @Query(
            "SELECT * FROM score_definitions " +
                    "WHERE severity = :severity " +
                    "AND active = 1 " +
                    "ORDER BY score ASC"
    )
    List<ScoreDefinitionEntity> getActiveBySeverity(
            String severity
    );

    @Query(
            "SELECT COUNT(*) FROM score_definitions"
    )
    int count();

    @Query(
            "SELECT COUNT(*) FROM score_definitions " +
                    "WHERE active = 1"
    )
    int countActive();

    @Query(
            "DELETE FROM score_definitions"
    )
    void deleteAll();
}