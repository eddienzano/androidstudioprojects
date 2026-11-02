package com.aaagrowers.scoutingapp.data.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointBedEntity;

import java.util.List;

@Dao
public interface ScanningPointBedDao {

    /*
     * ================================================================
     * INSERT
     * ================================================================
     */

    @Insert
    void insert(
            ScanningPointBedEntity relationship
    );


    @Insert
    void insertAll(
            List<ScanningPointBedEntity> relationships
    );


    /*
     * ================================================================
     * GET BEDS FOR SCANNING POINT
     * ================================================================
     */

    @Query(
            "SELECT * FROM scanning_point_beds " +
                    "WHERE scanning_point_id = :scanningPointId " +
                    "ORDER BY bed_id ASC"
    )
    List<ScanningPointBedEntity> getBedsForScanningPoint(
            int scanningPointId
    );


    /*
     * ================================================================
     * GET SCANNING POINTS FOR BED
     * ================================================================
     */

    @Query(
            "SELECT * FROM scanning_point_beds " +
                    "WHERE bed_id = :bedId"
    )
    List<ScanningPointBedEntity> getScanningPointsForBed(
            int bedId
    );


    /*
     * ================================================================
     * COUNT BEDS FOR SCANNING POINT
     * ================================================================
     */

    @Query(
            "SELECT COUNT(*) FROM scanning_point_beds " +
                    "WHERE scanning_point_id = :scanningPointId"
    )
    int countBedsForScanningPoint(
            int scanningPointId
    );


    /*
     * ================================================================
     * COUNT ALL
     * ================================================================
     */

    @Query(
            "SELECT COUNT(*) FROM scanning_point_beds"
    )
    int count();


    /*
     * ================================================================
     * DELETE ALL
     * ================================================================
     */

    @Query(
            "DELETE FROM scanning_point_beds"
    )
    void deleteAll();


    /*
     * ================================================================
     * DELETE FOR SCANNING POINT
     * ================================================================
     */

    @Query(
            "DELETE FROM scanning_point_beds " +
                    "WHERE scanning_point_id = :scanningPointId"
    )
    void deleteForScanningPoint(
            int scanningPointId
    );


    /*
     * ================================================================
     * GET BY SCANNING POINT
     *
     * Kept as a separate method because ScoutingActivity
     * uses this name.
     * ================================================================
     */

    @Query(
            "SELECT * FROM scanning_point_beds " +
                    "WHERE scanning_point_id = :scanningPointId " +
                    "ORDER BY bed_id ASC"
    )
    List<ScanningPointBedEntity> getByScanningPoint(
            int scanningPointId
    );
}