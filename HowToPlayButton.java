import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)

public class HowToPlayButton extends Button
{
    public HowToPlayButton(String path){
        super(path);
    }

    public void onButtonClicked(){
        Greenfoot.setWorld(new HowToPlayMenu());
    }
}
