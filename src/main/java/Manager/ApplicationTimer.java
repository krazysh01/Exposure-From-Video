package Manager;

import javafx.animation.AnimationTimer;

import java.util.ArrayList;

public class ApplicationTimer extends AnimationTimer {

    private static ApplicationTimer instance = new ApplicationTimer();

    public static ApplicationTimer getInstance(){return instance;}

    private ArrayList<Updatable> updatables;

    private ApplicationTimer(){
        updatables = new ArrayList<>();
        start();
    }

    @Override
    public void handle(long now) {
        for(Updatable u : updatables){
            u.Update(now);
        }
    }

    public boolean register(Updatable u){
        return updatables.add(u);
    }

    public boolean unregister(Updatable u){
        return updatables.remove(u);
    }

}
