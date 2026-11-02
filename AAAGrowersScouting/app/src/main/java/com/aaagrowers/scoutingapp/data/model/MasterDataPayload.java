package com.aaagrowers.scoutingapp.data.model;

import com.aaagrowers.scoutingapp.data.database.entity.BayEntity;
import com.aaagrowers.scoutingapp.data.database.entity.BedEntity;
import com.aaagrowers.scoutingapp.data.database.entity.FarmEntity;
import com.aaagrowers.scoutingapp.data.database.entity.FcmTrapEntity;
import com.aaagrowers.scoutingapp.data.database.entity.GreenhouseEntity;
import com.aaagrowers.scoutingapp.data.database.entity.InsectTypeEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ObservationTypeEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointBedEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScanningPointEntity;
import com.aaagrowers.scoutingapp.data.database.entity.ScoreDefinitionEntity;
import com.aaagrowers.scoutingapp.data.database.entity.SectionEntity;

import java.util.List;

public class MasterDataPayload {

    public List<FarmEntity> farms;

    public List<SectionEntity> sections;

    public List<GreenhouseEntity> greenhouses;

    public List<BayEntity> bays;

    public List<BedEntity> beds;

    public List<ScanningPointEntity> scanning_points;

    public List<ScanningPointBedEntity> scanning_point_beds;

    public List<FcmTrapEntity> fcm_traps;

    public List<InsectTypeEntity> insect_types;

    public List<ObservationTypeEntity> observation_types;

    public List<ScoreDefinitionEntity> score_definitions;
}