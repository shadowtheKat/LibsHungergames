package me.libraryaddict.Hungergames.Events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
 
public class TimeSecondEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
 
    public TimeSecondEvent() {
    }
 
    public HandlerList getHandlers() {
        return handlers;
    }
 
    public static HandlerList getHandlerList() {
        return handlers;
    }
}