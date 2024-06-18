package com.smov.gabriel.orientatree.model;

import java.util.ArrayList;
import java.util.Date;

public class participationGiver {

    public static ArrayList<Participation> getParticipationGeneral(){
        ArrayList<Participation> Participaciones = new ArrayList<Participation>();
        //Participacion Ricardo
        Participation Participacion1 = new Participation("Ricardo");
        Participacion1.setState(ParticipationState.FINISHED);
        Date Inicio= new Date(2024,10,2,23,59,58);
        Date Fin= new Date(2024,10,2,23,59,59);
        Participacion1.setStartTime(Inicio);
        Participacion1.setFinishTime(Fin);
        Participacion1.setCompleted(true);
        ArrayList<BeaconReached> Balizas1= new ArrayList<BeaconReached>();
        Date Reached= new Date(2024,10,2,23,59,59);
        BeaconReached Baliza1= new BeaconReached(Reached,"Beacon1",1,"Pepito1",true,true);
        BeaconReached Baliza2= new BeaconReached(Reached,"Beacon2",2,"Pepito2",false,true);
        BeaconReached Baliza3= new BeaconReached(Reached,"Beacon3",3,"Pepito3",true,true);
        BeaconReached Baliza4= new BeaconReached(Reached,"Beacon4",4,"Pepito4",false,true);
        BeaconReached Baliza5= new BeaconReached(Reached,"Beacon4",false);
        Balizas1.add(Baliza1);
        Balizas1.add(Baliza2);
        Balizas1.add(Baliza3);
        Balizas1.add(Baliza4);
        Balizas1.add(Baliza5);
        Participacion1.setReaches(Balizas1);
        Participaciones.add(Participacion1);


        //Participacion Nestor
        Participation Participacion2 = new Participation("Nestor");
        Participacion2.setStartTime(Inicio);
        Participacion2.setCompleted(false);
        Participacion2.setState(ParticipationState.NOW);
        ArrayList<BeaconReached> Balizas2= new ArrayList<BeaconReached>();
        Date Reached2= new Date(2024,10,2,23,59,59);
        BeaconReached Baliza21= new BeaconReached(Reached2,"Beacon1",1,"Pepito1",false,true);
        BeaconReached Baliza22= new BeaconReached(Reached2,"Beacon2",2,"Pepito2",false,true);
        BeaconReached Baliza23= new BeaconReached(Reached2,"Beacon4",false);
        Balizas2.add(Baliza21);
        Balizas2.add(Baliza22);
        Balizas2.add(Baliza23);
        Participacion2.setReaches(Balizas2);
        Participaciones.add(Participacion2);


        Participation Participacion3 = new Participation("Carla");
        Participacion3.setStartTime(new Date(2024, 5, 15, 12, 0, 0));
        Participacion3.setCompleted(true);
        Participacion3.setState(ParticipationState.NOW);
        ArrayList<BeaconReached> Balizas3 = new ArrayList<BeaconReached>();
        Date Reached3 = new Date(2024, 5, 15, 14, 30, 0);
        BeaconReached Baliza31 = new BeaconReached(Reached3, "Beacon3", 3, "Pepito3", true, false);
        BeaconReached Baliza32 = new BeaconReached(Reached3, "Beacon4", 4, "Pepito4", true, false);
        BeaconReached Baliza33 = new BeaconReached(Reached3, "Beacon5", true);
        Balizas3.add(Baliza31);
        Balizas3.add(Baliza32);
        Balizas3.add(Baliza33);
        Participacion3.setReaches(Balizas3);
        Participaciones.add(Participacion3);

        Participation Participacion4 = new Participation("Luis");
        Participacion4.setStartTime(new Date(2024, 7, 20, 9, 15, 0));
        Participacion4.setCompleted(false);
        Participacion4.setState(ParticipationState.NOW);
        ArrayList<BeaconReached> Balizas4 = new ArrayList<BeaconReached>();
        Date Reached4 = new Date(2024, 7, 20, 11, 45, 0);
        BeaconReached Baliza41 = new BeaconReached(Reached4, "Beacon6", 6, "Pepito6", false, true);
        BeaconReached Baliza42 = new BeaconReached(Reached4, "Beacon7", 7, "Pepito7", false, true);
        BeaconReached Baliza43 = new BeaconReached(Reached4, "Beacon8", false);
        Balizas4.add(Baliza41);
        Balizas4.add(Baliza42);
        Balizas4.add(Baliza43);
        Participacion4.setReaches(Balizas4);
        Participaciones.add(Participacion4);

        Participation Participacion5 = new Participation("Marta");
        Participacion5.setStartTime(new Date(2024, 3, 10, 8, 30, 0));
        Participacion5.setCompleted(true);
        Participacion5.setState(ParticipationState.FINISHED);
        ArrayList<BeaconReached> Balizas5 = new ArrayList<BeaconReached>();
        Date Reached5 = new Date(2024, 3, 10, 10, 0, 0);
        BeaconReached Baliza51 = new BeaconReached(Reached5, "Beacon9", 9, "Pepito9", true, false);
        BeaconReached Baliza52 = new BeaconReached(Reached5, "Beacon10", 10, "Pepito10", true, false);
        BeaconReached Baliza53 = new BeaconReached(Reached5, "Beacon11", true);
        Balizas5.add(Baliza51);
        Balizas5.add(Baliza52);
        Balizas5.add(Baliza53);
        Participacion5.setReaches(Balizas5);
        Participaciones.add(Participacion5);

        return Participaciones;
    }
}
